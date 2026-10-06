package commonly.commonlybe.global.error.response;

import commonly.commonlybe.global.error.error_code.ErrorProperty;
import lombok.Builder;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Builder
public record ErrorResponse(
    String code,
    int status,
    LocalDateTime timestamp,
    String message
) {
    /** HttpStatus enum에 없는 상태코드의 code 접두사. 예: 599 -> HTTP_599 */
    private static final String UNKNOWN_STATUS_CODE_PREFIX = "HTTP_";

    /**
     * ErrorProperty가 없는 경로(Spring MVC 내부 예외 등). 상태코드 이름을 code로 쓴다.
     *
     * resolve()를 쓰는 이유: ResponseStatusException은 HttpStatus enum에 없는 raw 상태코드로도
     * 만들 수 있다. valueOf()는 그 경우 예외를 던지고, 에러 응답을 만드는 중에 터진다.
     */
    public static ErrorResponse of(Exception e, int status) {
        HttpStatus resolved = HttpStatus.resolve(status);
        return ErrorResponse.builder()
            .code(resolved != null ? resolved.name() : UNKNOWN_STATUS_CODE_PREFIX + status)
            .status(status)
            .timestamp(LocalDateTime.now())
            .message(e.getMessage())
            .build();
    }

    public static ErrorResponse of(ErrorProperty errorProperty, String message) {
        return ErrorResponse.builder()
            .code(errorProperty.getCode())
            .status(errorProperty.getStatus().value())
            .timestamp(LocalDateTime.now())
            .message(message)
            .build();
    }

    public static ErrorResponse from(ErrorProperty errorProperty) {
        return ErrorResponse.builder()
            .code(errorProperty.getCode())
            .status(errorProperty.getStatus().value())
            .timestamp(LocalDateTime.now())
            .message(errorProperty.getMessage())
            .build();
    }
}
