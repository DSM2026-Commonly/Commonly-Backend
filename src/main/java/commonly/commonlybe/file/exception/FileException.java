package commonly.commonlybe.file.exception;

import commonly.commonlybe.global.error.error_code.FileErrorCode;
import commonly.commonlybe.global.error.exception.CommonlyException;

public class FileException extends CommonlyException {

    public FileException(FileErrorCode fileErrorCode) {
        super(fileErrorCode);
    }

    public FileException(FileErrorCode fileErrorCode, String message) {
        super(fileErrorCode, message);
    }
}
