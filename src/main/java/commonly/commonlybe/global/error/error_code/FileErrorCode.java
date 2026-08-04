package commonly.commonlybe.global.error.error_code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum FileErrorCode implements ErrorProperty {
    INVALID_DIVISION(HttpStatus.BAD_REQUEST, "구분 값이 허용되지 않습니다. (채용/전보/해지/퇴직)"),
    INVALID_EMPLOYMENT_TYPE(HttpStatus.BAD_REQUEST, "근무형태 값이 허용되지 않습니다. (기간제/단시간근로자)");

    private final HttpStatus status;
    private final String message;
}
