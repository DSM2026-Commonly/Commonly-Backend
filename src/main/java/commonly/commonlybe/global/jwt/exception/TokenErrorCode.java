package commonly.commonlybe.global.jwt.exception;

import commonly.commonlybe.global.error.error_code.ErrorProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum TokenErrorCode implements ErrorProperty {
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "이미 사용되었거나 무효화된 리프레시 토큰입니다.");

    private final HttpStatus status;
    private final String message;
}
