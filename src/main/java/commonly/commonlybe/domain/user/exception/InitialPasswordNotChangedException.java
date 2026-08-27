package commonly.commonlybe.domain.user.exception;

import commonly.commonlybe.global.error.exception.CommonlyException;

public class InitialPasswordNotChangedException extends CommonlyException {

    public InitialPasswordNotChangedException() {
        super(UserErrorCode.INITIAL_PASSWORD_NOT_CHANGED);
    }
}
