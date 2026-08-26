package commonly.commonlybe.file.excel;

import java.util.Set;

public final class ColumnMappingTable {

    private static final Set<String> VALID_TARGET_FIELDS = Set.of(
            "name", "birthDate", "gender", "jobTitle", "keyResponsibilities",
            "hireDate", "expirationDate", "retirementDate", "division", "reason",
            "employmentType", "note"
    );

    private static final Set<String> REQUIRED_TARGET_FIELDS = Set.of("name", "gender");

    private ColumnMappingTable() {
    }

    public static boolean isValidTargetField(String targetField) {
        return VALID_TARGET_FIELDS.contains(targetField);
    }

    public static Set<String> requiredTargetFields() {
        return REQUIRED_TARGET_FIELDS;
    }
}
