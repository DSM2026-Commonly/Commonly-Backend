package commonly.commonlybe.global.error.response;

import commonly.commonlybe.global.error.error_code.ErrorProperty;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ErrorResponse(
    int status,
    LocalDateTime timestamp,
    String message
) {
    public static ErrorResponse of(Exception e, int status) {
        return ErrorResponse.builder()
            .status(status)
            .timestamp(LocalDateTime.now())
            .message(e.getMessage())
            .build();
    }

    public static ErrorResponse of(ErrorProperty errorProperty, String message) {
        return ErrorResponse.builder()
            .status(errorProperty.getStatus().value())
            .timestamp(LocalDateTime.now())
            .message(message)
            .build();
    }

    public static ErrorResponse from(ErrorProperty errorProperty) {
        return ErrorResponse.builder()
            .status(errorProperty.getStatus().value())
            .timestamp(LocalDateTime.now())
            .message(errorProperty.getMessage())
            .build();
    }
}
