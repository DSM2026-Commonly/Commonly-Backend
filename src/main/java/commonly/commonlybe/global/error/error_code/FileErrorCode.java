package commonly.commonlybe.global.error.error_code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum FileErrorCode implements ErrorProperty {
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다. .xlsx 파일만 업로드할 수 있습니다."),
    STORAGE_FAILURE(HttpStatus.BAD_GATEWAY, "파일 저장소 처리 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
