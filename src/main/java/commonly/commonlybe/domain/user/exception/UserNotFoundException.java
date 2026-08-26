package commonly.commonlybe.domain.user.exception;

import commonly.commonlybe.global.error.exception.CommonlyException;

public class UserNotFoundException extends CommonlyException {

    public UserNotFoundException() {
        super(UserErrorCode.USER_NOT_FOUND);
    }
}