package commonly.commonlybe.domain.user.exception;

import commonly.commonlybe.global.error.exception.CommonlyException;

public class UserAlreadyExistsException extends CommonlyException {

    public UserAlreadyExistsException() {
        super(UserErrorCode.USER_ALREADY_EXISTS);
    }
}
