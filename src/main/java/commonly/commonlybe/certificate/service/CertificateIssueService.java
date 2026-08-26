package commonly.commonlybe.certificate.service;

import commonly.commonlybe.certificate.controller.dto.CertificateIssueRequest;
import commonly.commonlybe.certificate.controller.dto.CertificateIssueResponse;
import commonly.commonlybe.certificate.document.DocumentNumberGenerator;
import commonly.commonlybe.certificate.document.WorkPeriod;
import commonly.commonlybe.certificate.document.WorkPeriodCalculator;
import commonly.commonlybe.certificate.entity.CertificateEntity;
import commonly.commonlybe.certificate.entity.CertificateIssuedEntity;
import commonly.commonlybe.certificate.exception.CertificateErrorCode;
import commonly.commonlybe.certificate.exception.CertificateException;
import commonly.commonlybe.certificate.repository.CertificateIssuedRepository;
import commonly.commonlybe.certificate.repository.CertificateRepository;
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

@Service
@RequiredArgsConstructor
public class CertificateIssueService {

    private final HumanRepository humanRepository;
    private final CertificateRepository certificateRepository;
    private final CertificateIssuedRepository certificateIssuedRepository;
    private final DocumentNumberGenerator documentNumberGenerator;

    @Transactional
    public CertificateIssueResponse issue(CertificateIssueRequest request) {
        if (!humanRepository.existsById(request.humanId())) {
            throw new HumanException(HumanErrorCode.HUMAN_NOT_FOUND);
        }

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

        return new CertificateIssueResponse(
                issued.getCertificateIssuedId(),
                issued.getDocumentNo(),
                "/api/certificates/%d/download".formatted(issued.getCertificateIssuedId()));
    }
}
