package commonly.commonlybe.global.jwt.exception;

import commonly.commonlybe.global.error.exception.CommonlyException;

public class RefreshTokenNotFoundException extends CommonlyException {

    public RefreshTokenNotFoundException() {
        super(TokenErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }
}
