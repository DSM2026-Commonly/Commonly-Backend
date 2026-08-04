package commonly.commonlybe.certificate.entity;

import commonly.commonlybe.global.error.error_code.FileErrorCode;
import commonly.commonlybe.global.error.exception.CommonlyException;
import java.text.Normalizer;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmploymentType {

    FIXED_TERM("기간제"),
    PART_TIME("단시간근로자");

    private final String label;

    public static EmploymentType from(String raw) {
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFC).trim();
        return Arrays.stream(values())
                .filter(type -> type.label.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new CommonlyException(
                        FileErrorCode.INVALID_EMPLOYMENT_TYPE,
                        "근무형태 값 '%s'은 허용되지 않습니다 (기간제/단시간근로자)".formatted(raw)));
    }
}
