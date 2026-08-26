package commonly.commonlybe.certificate.controller.dto;

import commonly.commonlybe.certificate.entity.CertificateCodes;
import commonly.commonlybe.certificate.entity.Gender;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 재직 이력(certificate) 한 줄 수정. 이미 발급된 증명서 PDF는 불변이라 여기서 바꿔도 안 바뀐다.
 */
public record CertificateUpdateRequest(
        @NotBlank @Size(max = 255) String name,
        LocalDate birthDate,
        @NotNull Gender gender,
        @Size(max = 255) String jobTitle,
        @Size(max = 255) String keyResponsibilities,
        LocalDate hireDate,
        LocalDate expirationDate,
        LocalDate retirementDate,
        String division,
        @Size(max = 255) String department,
        String reason,
        String employmentType,
        String note
) {
    @AssertTrue(message = "구분 값은 채용/전보/해지/퇴직 중 하나여야 합니다.")
    public boolean isDivisionValid() {
        return division == null || CertificateCodes.VALID_DIVISIONS.contains(division);
    }

    @AssertTrue(message = "근무형태 값은 기간제/단시간근로자 중 하나여야 합니다.")
    public boolean isEmploymentTypeValid() {
        return employmentType == null || CertificateCodes.VALID_EMPLOYMENT_TYPES.contains(employmentType);
    }

    @AssertTrue(message = "채용일이 퇴직일보다 늦습니다.")
    public boolean isWorkPeriodValid() {
        return hireDate == null || retirementDate == null || !hireDate.isAfter(retirementDate);
    }
}
