package commonly.commonlybe.global.error.error_code;

import org.springframework.http.HttpStatus;

public interface ErrorProperty {
    HttpStatus getStatus();
    String getMessage();
}
