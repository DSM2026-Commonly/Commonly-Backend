package commonly.commonlybe.file.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record MappingConfirmRequest(
        @NotEmpty List<@Valid ColumnMapping> mappings,
        boolean confirmed
) {
}
