package commonly.commonlybe.file.excel;

import java.util.List;

public record ParsedExcel(List<String> headers, List<ParsedRow> rows) {
}
