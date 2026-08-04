package commonly.commonlybe.file.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record ColumnMapping(
        @NotBlank String sourceColumn,
        @NotBlank String targetField
) {
}
