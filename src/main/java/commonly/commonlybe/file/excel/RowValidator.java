package commonly.commonlybe.file.excel;

import commonly.commonlybe.certificate.entity.CertificateCodes;
import commonly.commonlybe.certificate.entity.CertificateEntity;
import commonly.commonlybe.certificate.entity.Gender;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RowValidator {

    /** 날짜로 저장되는 필드와 실패 메시지에 쓸 이름. 순서 고정(실패 메시지가 매번 같아야 한다). */
    private static final List<Map.Entry<String, String>> DATE_FIELDS = List.of(
            Map.entry("birthDate", "생년월일"),
            Map.entry("hireDate", "채용일"),
            Map.entry("expirationDate", "만료예정일"),
            Map.entry("retirementDate", "퇴직일"));

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
        if (division != null && !CertificateCodes.VALID_DIVISIONS.contains(division)) {
            return RowResult.failure("구분 값 '%s'은 허용되지 않습니다 (채용/전보/해지/퇴직)".formatted(division));
        }

        String employmentType = normalize(fieldValues.get("employmentType"));
        if (employmentType != null && !CertificateCodes.VALID_EMPLOYMENT_TYPES.contains(employmentType)) {
            return RowResult.failure(
                    "근무형태 값 '%s'은 허용되지 않습니다 (기간제/단시간근로자)".formatted(employmentType));
        }

        Map<String, LocalDate> dates = new HashMap<>();
        for (Map.Entry<String, String> dateField : DATE_FIELDS) {
            String raw = trimToNull(fieldValues.get(dateField.getKey()));
            LocalDate parsed = CellValueConverter.parseDate(raw);
            if (raw != null && parsed == null) {
                return RowResult.failure(
                        "%s 값 '%s'의 날짜 형식을 인식할 수 없습니다".formatted(dateField.getValue(), raw));
            }
            dates.put(dateField.getKey(), parsed);
        }

        CertificateEntity certificate = CertificateEntity.builder()
                .name(name)
                .birthDate(dates.get("birthDate"))
                .gender(gender)
                .jobTitle(trimToNull(fieldValues.get("jobTitle")))
                .keyResponsibilities(trimToNull(fieldValues.get("keyResponsibilities")))
                .hireDate(dates.get("hireDate"))
                .expirationDate(dates.get("expirationDate"))
                .retirementDate(dates.get("retirementDate"))
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
