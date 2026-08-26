package commonly.commonlybe.file.excel;

import commonly.commonlybe.file.exception.FileException;
import commonly.commonlybe.global.error.error_code.FileErrorCode;
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

    private ExcelParser() {
    }

    public static ParsedExcel parse(InputStream inputStream, int maxRows) {
        // DataFormatter는 스레드 안전하지 않으므로 호출마다 새로 만든다.
        DataFormatter dataFormatter = new DataFormatter();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new FileException(FileErrorCode.UNPROCESSABLE_FILE);
            }
            Sheet sheet = workbook.getSheetAt(0);

            int firstNonEmptyRow = findFirstNonEmptyRow(sheet, dataFormatter);
            if (firstNonEmptyRow == -1) {
                throw new FileException(FileErrorCode.UNPROCESSABLE_FILE);
            }
            if (firstNonEmptyRow != 0) {
                throw new FileException(FileErrorCode.INVALID_HEADER_ROW,
                        "1행에서 헤더를 찾을 수 없습니다. 데이터가 있는 첫 번째 행: %d행".formatted(firstNonEmptyRow + 1));
            }

            Row headerRow = sheet.getRow(0);
            int columnCount = headerRow.getLastCellNum();
            List<String> headers = extractRowValues(headerRow, columnCount, dataFormatter);

            List<ParsedRow> rows = new ArrayList<>();
            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) {
                    continue;
                }
                if (rows.size() == maxRows) {
                    throw new FileException(FileErrorCode.ROW_COUNT_EXCEEDED);
                }
                rows.add(new ParsedRow(rowNum + 1, extractRowValues(row, columnCount, dataFormatter)));
            }

            return new ParsedExcel(headers, rows);
        } catch (IOException e) {
            throw new FileException(FileErrorCode.UNPROCESSABLE_FILE);
        }
    }

    private static int findFirstNonEmptyRow(Sheet sheet, DataFormatter dataFormatter) {
        for (int rowNum = 0; rowNum <= sheet.getLastRowNum(); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row != null && hasAnyContent(row, dataFormatter)) {
                return rowNum;
            }
        }
        return -1;
    }

    private static boolean hasAnyContent(Row row, DataFormatter dataFormatter) {
        for (int cellNum = 0; cellNum < row.getLastCellNum(); cellNum++) {
            if (!cellValue(row, cellNum, dataFormatter).isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static List<String> extractRowValues(Row row, int columnCount, DataFormatter dataFormatter) {
        List<String> values = new ArrayList<>(columnCount);
        for (int cellNum = 0; cellNum < columnCount; cellNum++) {
            values.add(cellValue(row, cellNum, dataFormatter));
        }
        return values;
    }

    private static String cellValue(Row row, int cellNum, DataFormatter dataFormatter) {
        Cell cell = row.getCell(cellNum);
        return cell == null ? "" : dataFormatter.formatCellValue(cell);
    }
}
