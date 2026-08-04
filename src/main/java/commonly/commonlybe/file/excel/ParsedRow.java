package commonly.commonlybe.file.excel;

import java.util.List;

public record ParsedRow(int rowIndex, List<String> cells) {
}
