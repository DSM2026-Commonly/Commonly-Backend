package commonly.commonlybe.file.excel;

import commonly.commonlybe.global.error.error_code.FileErrorCode;
import commonly.commonlybe.global.error.exception.CommonlyException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class ExcelParser {

    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    private ExcelParser() {
    }

    public static ParsedExcel parse(InputStream inputStream) {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new CommonlyException(FileErrorCode.UNPROCESSABLE_FILE);
            }
            Sheet sheet = workbook.getSheetAt(0);

            int firstNonEmptyRow = findFirstNonEmptyRow(sheet);
            if (firstNonEmptyRow == -1) {
                throw new CommonlyException(FileErrorCode.UNPROCESSABLE_FILE);
            }
            if (firstNonEmptyRow != 0) {
                throw new CommonlyException(FileErrorCode.INVALID_HEADER_ROW,
                        "1행에서 헤더를 찾을 수 없습니다. 데이터가 있는 첫 번째 행: %d행".formatted(firstNonEmptyRow + 1));
            }

            Row headerRow = sheet.getRow(0);
            int columnCount = headerRow.getLastCellNum();
            List<String> headers = extractRowValues(headerRow, columnCount);

            List<ParsedRow> rows = new ArrayList<>();
            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) {
                    continue;
                }
                rows.add(new ParsedRow(rowNum + 1, extractRowValues(row, columnCount)));
            }

            return new ParsedExcel(headers, rows);
        } catch (IOException e) {
            throw new CommonlyException(FileErrorCode.UNPROCESSABLE_FILE);
        }
    }

    private static int findFirstNonEmptyRow(Sheet sheet) {
        for (int rowNum = 0; rowNum <= sheet.getLastRowNum(); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row != null && hasAnyContent(row)) {
                return rowNum;
            }
        }
        return -1;
    }

    private static boolean hasAnyContent(Row row) {
        for (int cellNum = 0; cellNum < row.getLastCellNum(); cellNum++) {
            if (!cellValue(row, cellNum).isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static List<String> extractRowValues(Row row, int columnCount) {
        List<String> values = new ArrayList<>(columnCount);
        for (int cellNum = 0; cellNum < columnCount; cellNum++) {
            values.add(cellValue(row, cellNum));
        }
        return values;
    }

    private static String cellValue(Row row, int cellNum) {
        Cell cell = row.getCell(cellNum);
        return cell == null ? "" : DATA_FORMATTER.formatCellValue(cell);
    }
}
