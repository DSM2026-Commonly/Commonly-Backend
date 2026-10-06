package commonly.commonlybe.file.excel;

import java.util.Set;

public final class ColumnMappingTable {

    /**
     * department와 address는 선택이다. 해당 열이 없는 기존 운영 엑셀도 그대로 올라가야 하므로
     * 필수로 올리면 안 된다 (certificate-domain.md §1-2).
     *
     * address는 certificate가 아니라 humans로 들어간다 — 엑셀로 처음 등장한 대상자의
     * 인적사항을 만들 때 쓴다. 매핑하지 않으면 증명서 서식의 주소 칸이 공란으로 찍힌다.
     */
    private static final Set<String> VALID_TARGET_FIELDS = Set.of(
            "name", "birthDate", "gender", "address", "jobTitle", "keyResponsibilities",
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
