package commonly.commonlybe.file.excel;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@RequiredArgsConstructor
public final class CellValueConverter {

    private static final List<DateTimeFormatter> FULL_YEAR_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
            DateTimeFormatter.ofPattern("yyyy년 M월 d일")
    );

    private static final List<DateTimeFormatter> TWO_DIGIT_YEAR_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yy-MM-dd"),
            DateTimeFormatter.ofPattern("yy.MM.dd")
    );

    /**
     * 지원 패턴으로 파싱을 시도한다. 실패하면 null을 반환한다(호출자가 실패 사유를 구성).
     */
    public static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();

        LocalDate fullYear = tryParse(trimmed, FULL_YEAR_FORMATTERS);
        if (fullYear != null) {
            return fullYear;
        }

        LocalDate twoDigitYear = tryParse(trimmed, TWO_DIGIT_YEAR_FORMATTERS);
        return twoDigitYear == null ? null : slideIntoPastCentury(twoDigitYear);
    }

    private static LocalDate tryParse(String trimmed, List<DateTimeFormatter> formatters) {
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
                // 다음 패턴 시도
            }
        }
        return null;
    }

    /**
     * "yy"는 2000~2099로만 해석되므로, 미래 날짜인 경우에만 직전 세기로 옮긴다.
     * 연도가 4자리로 명시된 값에는 적용하지 않는다.
     */
    private static LocalDate slideIntoPastCentury(LocalDate date) {
        return date.isAfter(LocalDate.now()) ? date.minusYears(100) : date;
    }
}
