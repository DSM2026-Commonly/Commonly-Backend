package commonly.commonlybe.global.error.response;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Builder
public record ValidationErrorResponse(
    String code,
    int status,
    LocalDateTime timestamp,
    Map<String, String> error
) {
    /** 검증 실패는 원인이 error 맵에 필드별로 담기므로 code는 한 종류다. */
    private static final String CODE = "VALIDATION_FAILED";

    public static ValidationErrorResponse of(BindException e) {
        Map<String, String> filedErrors = new HashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            filedErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ValidationErrorResponse.builder()
            .code(CODE)
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(LocalDateTime.now())
            .error(filedErrors)
            .build();
    }

    public static ValidationErrorResponse of(ConstraintViolationException e) {
        Map<String, String> filedErrors = new HashMap<>();
        for (ConstraintViolation<?> fieldError : e.getConstraintViolations()) {
            String path = fieldError.getPropertyPath().toString().split("\\.")[0];
            filedErrors.put(path, fieldError.getMessage());
        }

        return ValidationErrorResponse.builder()
            .code(CODE)
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(LocalDateTime.now())
            .error(filedErrors)
            .build();
    }
}
