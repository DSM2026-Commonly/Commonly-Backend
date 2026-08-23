package commonly.commonlybe.file.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record ColumnMapping(
        @NotBlank(message = "소스컬럼이 비어있습니다.") String sourceColumn,
        @NotBlank(message = "목표컬럼이 비어있습니다.") String targetField
) {
}
