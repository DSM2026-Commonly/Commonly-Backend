package commonly.commonlybe.global.error.exception;

import commonly.commonlybe.global.error.error_code.ErrorProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class CommonlyException extends RuntimeException {
    private final ErrorProperty errorProperty;

    public CommonlyException(ErrorProperty errorProperty) {
        super(errorProperty.getMessage());
        this.errorProperty = errorProperty;
    }

    public CommonlyException(ErrorProperty errorProperty, String message) {
        super(message);
        this.errorProperty = errorProperty;
    }
}
