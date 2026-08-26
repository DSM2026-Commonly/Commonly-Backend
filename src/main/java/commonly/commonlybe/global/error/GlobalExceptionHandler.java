package commonly.commonlybe.global.error;

import commonly.commonlybe.global.error.error_code.ErrorProperty;
import commonly.commonlybe.global.error.exception.CommonlyException;
import commonly.commonlybe.global.error.response.ErrorResponse;
import commonly.commonlybe.global.error.response.ValidationErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(CommonlyException.class)
    protected ResponseEntity<ErrorResponse> handleCommonlyException(CommonlyException ex) {
        ErrorProperty errorProperty = ex.getErrorProperty();
        return ResponseEntity
            .status(errorProperty.getStatus())
            .body(ErrorResponse.of(errorProperty, ex.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ValidationErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        return ResponseEntity
            .badRequest()
            .body(ValidationErrorResponse.of(ex));
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity
            .badRequest()
            .headers(headers)
            .body(ValidationErrorResponse.of(ex));
    }

    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ValidationErrorResponse> handleBindException(BindException ex) {
        return ResponseEntity
            .badRequest()
            .body(ValidationErrorResponse.of(ex));
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        return ResponseEntity
            .status(statusCode)
            .headers(headers)
            .body(ErrorResponse.of(ex, statusCode.value()));
    }
}
