package commonly.commonlybe.certificate.service;

import commonly.commonlybe.certificate.controller.dto.CertificateIssueRequest;
import commonly.commonlybe.certificate.controller.dto.CertificateIssueResponse;
import commonly.commonlybe.certificate.document.CertificatePdfRenderer;
import commonly.commonlybe.certificate.document.DocumentNumberGenerator;
import commonly.commonlybe.certificate.document.WorkPeriod;
import commonly.commonlybe.certificate.document.WorkPeriodCalculator;
import commonly.commonlybe.certificate.entity.CertificateEntity;
import commonly.commonlybe.certificate.entity.CertificateIssuedEntity;
import commonly.commonlybe.certificate.exception.CertificateErrorCode;
import commonly.commonlybe.certificate.exception.CertificateException;
import commonly.commonlybe.certificate.repository.CertificateIssuedRepository;
import commonly.commonlybe.certificate.repository.CertificateRepository;
import commonly.commonlybe.global.s3.S3Uploader;
import commonly.commonlybe.human.entity.HumanEntity;
import commonly.commonlybe.human.exception.HumanErrorCode;
import commonly.commonlybe.human.exception.HumanException;
import commonly.commonlybe.human.repository.HumanRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class CertificateIssueService {

    private final HumanRepository humanRepository;
    private final CertificateRepository certificateRepository;
    private final CertificateIssuedRepository certificateIssuedRepository;
    private final DocumentNumberGenerator documentNumberGenerator;
    private final CertificatePdfRenderer certificatePdfRenderer;
    private final S3Uploader s3Uploader;

    @Transactional
    public CertificateIssueResponse issue(CertificateIssueRequest request) {
        HumanEntity human = humanRepository.findById(request.humanId())
                .orElseThrow(() -> new HumanException(HumanErrorCode.HUMAN_NOT_FOUND));

        // humanId 조건이 핵심이다. 빼면 남의 재직 이력이 증명서에 찍힌다.
        List<CertificateEntity> certificates =
                certificateRepository.findAllByCertificateIdInAndHumanIdOrderByHireDateAscCertificateIdAsc(
                        request.certificateIds(), request.humanId());
        if (certificates.size() != new HashSet<>(request.certificateIds()).size()) {
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_NOT_FOUND);
        }

        WorkPeriod total = WorkPeriodCalculator.totalOf(certificates);

        CertificateIssuedEntity issued = CertificateIssuedEntity.builder()
                .humanId(request.humanId())
                .documentNo(documentNumberGenerator.generate(LocalDate.now().getYear()))
                .purpose(request.purpose())
                .otherMatters(request.otherMatters())
                .totalMonths(total.months())
                .totalDays(total.days())
                .issuedAt(LocalDateTime.now())
                .certificateIds(certificates.stream().map(CertificateEntity::getCertificateId).toList())
                .build();
        certificateIssuedRepository.save(issued);

        // PDF 없이 발급 건만 남으면 201을 받고도 다운로드가 404다. 렌더나 업로드가 실패하면 발급 자체를 롤백한다.
        byte[] pdf = certificatePdfRenderer.render(issued, human, certificates);
        String key = "issued/certificates/%d/%d.pdf".formatted(
                issued.getIssuedAt().getYear(), issued.getCertificateIssuedId());
        s3Uploader.upload(key, pdf, "application/pdf");
        deleteOnRollback(key);
        issued.attachFile(key);

        return new CertificateIssueResponse(
                issued.getCertificateIssuedId(),
                issued.getDocumentNo(),
                "/api/certificates/%d/download".formatted(issued.getCertificateIssuedId()));
    }

    /** 업로드 뒤 커밋이 실패하면 어느 발급 건에도 안 걸린 PDF가 버킷에 남는다. */
    private void deleteOnRollback(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    s3Uploader.delete(key);
                }
            }
        });
    }
}
