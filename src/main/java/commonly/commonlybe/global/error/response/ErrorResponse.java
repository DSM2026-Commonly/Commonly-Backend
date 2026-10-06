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
    /**
     * ErrorProperty가 없는 경로(Spring MVC 내부 예외 등). 상태코드 이름을 code로 쓴다.
     */
    public static ErrorResponse of(Exception e, int status) {
        return ErrorResponse.builder()
            .code(HttpStatus.valueOf(status).name())
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
