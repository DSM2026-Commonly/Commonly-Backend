package commonly.commonlybe.file.controller.dto;

import java.util.List;

public record FileUploadResponse(Long fileId, String fileName, List<String> columns, List<ExcelRowDto> rows) {
}
