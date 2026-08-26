package commonly.commonlybe.human.exception;

import commonly.commonlybe.global.error.exception.CommonlyException;

public class HumanException extends CommonlyException {

    public HumanException(HumanErrorCode humanErrorCode) {
        super(humanErrorCode);
    }

    public HumanException(HumanErrorCode humanErrorCode, String message) {
        super(humanErrorCode, message);
    }
}
