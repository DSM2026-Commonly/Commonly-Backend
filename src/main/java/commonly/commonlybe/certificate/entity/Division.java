package commonly.commonlybe.certificate.entity;

import commonly.commonlybe.global.error.error_code.FileErrorCode;
import commonly.commonlybe.global.error.exception.CommonlyException;
import java.text.Normalizer;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Division {

    HIRE("채용"),
    TRANSFER("전보"),
    TERMINATE("해지"),
    RETIRE("퇴직");

    private final String label;

    public static Division from(String raw) {
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFC).trim();
        return Arrays.stream(values())
                .filter(division -> division.label.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new CommonlyException(
                        FileErrorCode.INVALID_DIVISION,
                        "구분 값 '%s'은 허용되지 않습니다 (채용/전보/해지/퇴직)".formatted(raw)));
    }
}
