package commonly.commonlybe.file.controller.dto;

import java.util.List;

public record MappingConfirmResponse(boolean saved, int insertedCount, List<FailedRowDto> failedRows) {
}
