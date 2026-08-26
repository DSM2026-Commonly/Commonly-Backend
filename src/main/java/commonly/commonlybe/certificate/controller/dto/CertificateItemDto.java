package commonly.commonlybe.certificate.controller.dto;

import commonly.commonlybe.certificate.entity.CertificateEntity;
import java.time.LocalDate;

/** 재직 이력 한 줄. 발급 상세(§5.3)와 경력 사항 찾기(§5.4)가 같은 모양을 쓴다. */
public record CertificateItemDto(
        Long certificateId,
        String division,
        String department,
        String employmentType,
        String jobTitle,
        String keyResponsibilities,
        LocalDate hireDate,
        LocalDate retirementDate,
        LocalDate expirationDate,
        String reason,
        String note
) {
    public static CertificateItemDto from(CertificateEntity certificate) {
        return new CertificateItemDto(
                certificate.getCertificateId(),
                certificate.getDivision(),
                certificate.getDepartment(),
                certificate.getEmploymentType(),
                certificate.getJobTitle(),
                certificate.getKeyResponsibilities(),
                certificate.getHireDate(),
                certificate.getRetirementDate(),
                certificate.getExpirationDate(),
                certificate.getReason(),
                certificate.getNote());
    }
}
