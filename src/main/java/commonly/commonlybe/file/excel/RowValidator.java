package commonly.commonlybe.file.excel;

import commonly.commonlybe.certificate.entity.CertificateEntity;
import commonly.commonlybe.certificate.entity.Gender;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public final class RowValidator {

    private static final Set<String> VALID_DIVISIONS = Set.of("채용", "전보", "해지", "퇴직");
    private static final Set<String> VALID_EMPLOYMENT_TYPES = Set.of("기간제", "단시간근로자");

    private RowValidator() {
    }

    public static RowResult validate(Map<String, String> fieldValues) {
        if (fieldValues.values().stream().allMatch(RowValidator::isBlank)) {
            return RowResult.skip();
        }

        String name = trimToNull(fieldValues.get("name"));
        if (name == null) {
            return RowResult.failure("성명이 비어 있습니다");
        }

        Gender gender = Gender.from(normalize(fieldValues.get("gender")));
        if (gender == null) {
            return RowResult.failure("성별 값을 인식할 수 없습니다 (남/여)");
        }

        String division = normalize(fieldValues.get("division"));
        if (division != null && !VALID_DIVISIONS.contains(division)) {
            return RowResult.failure("구분 값 '%s'은 허용되지 않습니다 (채용/전보/해지/퇴직)".formatted(division));
        }

        String employmentType = normalize(fieldValues.get("employmentType"));
        if (employmentType != null && !VALID_EMPLOYMENT_TYPES.contains(employmentType)) {
            return RowResult.failure(
                    "근무형태 값 '%s'은 허용되지 않습니다 (기간제/단시간근로자)".formatted(employmentType));
        }

        String birthDateRaw = trimToNull(fieldValues.get("birthDate"));
        LocalDate birthDate = CellValueConverter.parseDate(birthDateRaw);
        if (birthDateRaw != null && birthDate == null) {
            return RowResult.failure("생년월일 값 '%s'의 날짜 형식을 인식할 수 없습니다".formatted(birthDateRaw));
        }

        String hireDateRaw = trimToNull(fieldValues.get("hireDate"));
        String expirationDateRaw = trimToNull(fieldValues.get("expirationDate"));
        String retirementDateRaw = trimToNull(fieldValues.get("retirementDate"));

        LocalDate hireDate = CellValueConverter.parseDate(hireDateRaw);
        LocalDate expirationDate = CellValueConverter.parseDate(expirationDateRaw);
        LocalDate retirementDate = CellValueConverter.parseDate(retirementDateRaw);

        if (hireDate != null && expirationDate != null && hireDate.isAfter(expirationDate)) {
            return RowResult.failure(
                    "채용일(%s)이 만료예정일(%s)보다 늦습니다".formatted(hireDateRaw, expirationDateRaw));
        }
        if (hireDate != null && retirementDate != null && hireDate.isAfter(retirementDate)) {
            return RowResult.failure(
                    "채용일(%s)이 퇴직일(%s)보다 늦습니다".formatted(hireDateRaw, retirementDateRaw));
        }

        CertificateEntity certificate = CertificateEntity.builder()
                .name(name)
                .birthDate(birthDate)
                .gender(gender)
                .jobTitle(trimToNull(fieldValues.get("jobTitle")))
                .keyResponsibilities(trimToNull(fieldValues.get("keyResponsibilities")))
                .hireDate(hireDateRaw)
                .expirationDate(expirationDateRaw)
                .retirementDate(retirementDateRaw)
                .division(division)
                .reason(trimToNull(fieldValues.get("reason")))
                .employmentType(employmentType)
                .note(trimToNull(fieldValues.get("note")))
                .build();

        return RowResult.success(certificate);
    }

    private static String normalize(String raw) {
        String trimmed = trimToNull(raw);
        return trimmed == null ? null : Normalizer.normalize(trimmed, Normalizer.Form.NFC);
    }

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isBlank(String raw) {
        return raw == null || raw.isBlank();
    }
}
