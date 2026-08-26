package commonly.commonlybe.human.exception;

import commonly.commonlybe.global.error.error_code.ErrorProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum HumanErrorCode implements ErrorProperty {
    HUMAN_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 인적사항을 찾을 수 없습니다."),
    DUPLICATE_HUMAN(HttpStatus.CONFLICT, "성명과 생년월일이 동일한 인적사항이 이미 존재합니다.");

    private final HttpStatus status;
    private final String message;
}
