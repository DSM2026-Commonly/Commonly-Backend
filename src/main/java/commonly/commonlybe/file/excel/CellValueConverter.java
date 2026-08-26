package commonly.commonlybe.file.excel;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@RequiredArgsConstructor
public final class CellValueConverter {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
            DateTimeFormatter.ofPattern("yy-MM-dd"),
            DateTimeFormatter.ofPattern("yy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy년 M월 d일")
    );

    /**
     * 지원 패턴으로 파싱을 시도한다. 실패하면 null을 반환한다(호출자가 실패 사유를 구성).
     */
    public static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return correctTwoDigitYear(LocalDate.parse(trimmed, formatter));
            } catch (DateTimeParseException ignored) {
                // 다음 패턴 시도
            }
        }
        return null;
    }

    private static LocalDate correctTwoDigitYear(LocalDate date) {
        return date.isAfter(LocalDate.now()) ? date.minusYears(100) : date;
    }
}
