package commonly.commonlybe.global.jwt.exception;

import commonly.commonlybe.global.error.exception.CommonlyException;

public class InvalidTokenException extends CommonlyException {

    public InvalidTokenException() {
        super(TokenErrorCode.INVALID_TOKEN);
    }
}
