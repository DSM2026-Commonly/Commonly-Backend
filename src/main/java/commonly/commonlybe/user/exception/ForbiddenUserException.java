package commonly.commonlybe.user.exception;

import commonly.commonlybe.global.error.exception.CommonlyException;

public class ForbiddenUserException extends CommonlyException {

    public ForbiddenUserException() {
        super(UserErrorCode.FORBIDDEN_USER);
    }
}
