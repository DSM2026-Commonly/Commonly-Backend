package commonly.commonlybe.global.jwt.exception;

import commonly.commonlybe.global.error.exception.CommonlyException;

public class ExpiredTokenException extends CommonlyException {

    public ExpiredTokenException() {
        super(TokenErrorCode.EXPIRED_TOKEN);
    }
}
