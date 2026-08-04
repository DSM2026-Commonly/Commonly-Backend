package commonly.commonlybe.global.error.error_code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum FileErrorCode implements ErrorProperty {
    STORAGE_FAILURE(HttpStatus.BAD_GATEWAY, "파일 저장소 처리 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
