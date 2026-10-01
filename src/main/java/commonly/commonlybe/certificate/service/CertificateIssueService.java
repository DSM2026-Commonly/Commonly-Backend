package commonly.commonlybe.certificate.service;

import commonly.commonlybe.certificate.controller.dto.CertificateIssueRequest;
import commonly.commonlybe.certificate.controller.dto.CertificateIssueResponse;
import commonly.commonlybe.certificate.document.CertificatePdfRenderer;
import commonly.commonlybe.certificate.document.CertificatePdfRenderer.CertificateDocument;
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

    /**
     * PDF를 만들어 올리지 못하면 발급 자체를 실패시킨다. 파일 없는 발급 건이 201로 나가면
     * 다운로드가 404라 사용자가 재발급만 반복한다 (#34).
     *
     * ponytail: 렌더와 업로드가 채번 행 잠금을 잡은 채로 돈다. 창구 발급량에선 문제없음.
     */
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
        LocalDateTime issuedAt = LocalDateTime.now();
        String documentNo = documentNumberGenerator.generate(issuedAt.getYear());

        byte[] pdf = certificatePdfRenderer.render(new CertificateDocument(
                documentNo, human, certificates, total, request.purpose(), request.otherMatters(),
                issuedAt.toLocalDate()));
        String filePath = s3Uploader.upload(pdf, fileKey(issuedAt.getYear(), documentNo), "application/pdf");
        deleteOnRollback(filePath);

        CertificateIssuedEntity issued = CertificateIssuedEntity.builder()
                .humanId(human.getHumanId())
                .documentNo(documentNo)
                .purpose(request.purpose())
                .otherMatters(request.otherMatters())
                .totalMonths(total.months())
                .totalDays(total.days())
                .issuedAt(issuedAt)
                .filePath(filePath)
                .certificateIds(certificates.stream().map(CertificateEntity::getCertificateId).toList())
                .build();
        certificateIssuedRepository.save(issued);

        return new CertificateIssueResponse(
                issued.getCertificateIssuedId(),
                issued.getDocumentNo(),
                "/api/certificates/%d/download".formatted(issued.getCertificateIssuedId()));
    }

    /** 문서번호가 유일하니 key도 유일하다. 날짜 경로 대신 연도만 둬서 문서번호로 바로 찾을 수 있게 한다. */
    private static String fileKey(int year, String documentNo) {
        return "certificates/issued/%d/%s.pdf".formatted(year, documentNo);
    }

    /**
     * 업로드 뒤 DB가 롤백되면 S3에 주인 없는 PDF가 남는다. 커밋 시점 실패(ElementCollection flush 등)와
     * 본인 발급처럼 바깥 트랜잭션에서 터지는 경우까지 잡으려고 try/catch 대신 트랜잭션 완료 콜백에 건다.
     */
    private void deleteOnRollback(String key) {
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
