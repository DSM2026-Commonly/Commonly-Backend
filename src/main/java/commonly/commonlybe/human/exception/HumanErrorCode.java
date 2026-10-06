package commonly.commonlybe.human.exception;

import commonly.commonlybe.global.error.error_code.ErrorProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum HumanErrorCode implements ErrorProperty {
    HUMAN_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 인적사항을 찾을 수 없습니다."),
    DUPLICATE_HUMAN(HttpStatus.CONFLICT, "성명과 생년월일이 동일한 인적사항이 이미 존재합니다."),

    /**
     * 발급 기록은 공문서 대장이라 지우는 선택지가 없다. 그래서 안내도 "먼저 지우세요"가 아니다.
     */
    HUMAN_HAS_ISSUED_CERTIFICATE(HttpStatus.CONFLICT,
            "이미 발급된 경력증명서가 있어 인적사항을 삭제할 수 없습니다. 발급 이력은 보존 대상이므로 삭제 대신 인적사항 수정을 이용해 주세요."),

    HUMAN_HAS_CERTIFICATE(HttpStatus.CONFLICT,
            "등록된 재직 이력이 있어 인적사항을 삭제할 수 없습니다. 재직 이력을 먼저 삭제한 뒤 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String message;
}
