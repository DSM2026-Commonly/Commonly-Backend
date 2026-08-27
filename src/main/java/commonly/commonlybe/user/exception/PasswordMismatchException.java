package commonly.commonlybe.user.exception;

import commonly.commonlybe.global.error.exception.CommonlyException;

public class PasswordMismatchException extends CommonlyException {

    public PasswordMismatchException() {
        super(UserErrorCode.PASSWORD_MISMATCH);
    }
}
