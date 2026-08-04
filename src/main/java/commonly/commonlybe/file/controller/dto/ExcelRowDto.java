package commonly.commonlybe.file.controller.dto;

import java.util.List;

public record ExcelRowDto(int rowIndex, List<String> cells) {
}
