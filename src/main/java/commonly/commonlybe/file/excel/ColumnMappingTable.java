package commonly.commonlybe.file.excel;

import java.util.Set;

public final class ColumnMappingTable {

    /**
     * department는 선택이다. 부서 열이 없는 기존 운영 엑셀도 그대로 올라가야 하므로
     * 필수로 올리면 안 된다 (certificate-domain.md §1-2).
     */
    private static final Set<String> VALID_TARGET_FIELDS = Set.of(
            "name", "birthDate", "gender", "jobTitle", "keyResponsibilities",
            "hireDate", "expirationDate", "retirementDate", "division", "department",
            "reason", "employmentType", "note"
    );

    /** birthDate는 humans 매칭 키(성명, 생년월일)라 필수다. */
    private static final Set<String> REQUIRED_TARGET_FIELDS = Set.of("name", "birthDate", "gender");

    private ColumnMappingTable() {
    }

    public static boolean isValidTargetField(String targetField) {
        return VALID_TARGET_FIELDS.contains(targetField);
    }

    public static Set<String> requiredTargetFields() {
        return REQUIRED_TARGET_FIELDS;
    }
}
