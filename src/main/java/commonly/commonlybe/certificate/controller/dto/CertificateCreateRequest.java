package commonly.commonlybe.certificate.controller.dto;

import commonly.commonlybe.certificate.entity.CertificateCodes;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 재직 이력(certificate) 한 줄 등록. 개별 등록 화면이 POST /api/human으로 대상자를 만들거나 고른 뒤
 * 그 humanId로 호출한다.
 *
 * 명세 본문의 name/birthDate/gender는 일부러 받지 않고 humans 행에서 가져온다. 알 수 없는 필드는
 * Jackson 기본값(FAIL_ON_UNKNOWN_PROPERTIES=false)이 무시하므로 프론트가 계속 보내도 400은 안 난다.
 *
 * (1) 성별 표기가 두 벌이다 — human은 M/F(@JsonValue), certificate는 MALE/FEMALE(Jackson 기본).
 *     한 화면에서 표기가 두 번 바뀌면 프론트가 M을 보내는 순간 400이다.
 * (2) certificate.human_id 백필이 (name, birth_date) 일치 전제다 (certificate-domain.md §2-1).
 *     본문 값이 humans와 어긋나면 그 전제가 조용히 깨진다.
 */
public record CertificateCreateRequest(
        @NotNull Long humanId,
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
