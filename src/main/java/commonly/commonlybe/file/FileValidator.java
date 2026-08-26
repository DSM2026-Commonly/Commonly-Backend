package commonly.commonlybe.file;

import commonly.commonlybe.file.exception.FileException;
import commonly.commonlybe.global.error.error_code.FileErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.springframework.web.multipart.MultipartFile;

public final class FileValidator {

    private static final String REQUIRED_EXTENSION = ".xlsx";
    private static final byte[] ZIP_MAGIC_NUMBER = {0x50, 0x4B, 0x03, 0x04};

    private FileValidator() {
    }

    public static void validate(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(REQUIRED_EXTENSION)) {
            throw new FileException(FileErrorCode.UNSUPPORTED_FILE_TYPE);
        }
        if (!hasZipSignature(file)) {
            throw new FileException(FileErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    private static boolean hasZipSignature(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(ZIP_MAGIC_NUMBER.length);
            return Arrays.equals(header, ZIP_MAGIC_NUMBER);
        } catch (IOException e) {
            return false;
        }
    }
}
