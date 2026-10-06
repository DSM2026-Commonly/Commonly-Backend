package commonly.commonlybe.global.error.error_code;

import org.springframework.http.HttpStatus;

public interface ErrorProperty {
    HttpStatus getStatus();
    String getMessage();

    /**
     * 클라이언트가 분기에 쓰는 기계용 식별자. 모든 구현체가 enum이라 상수 이름을 그대로 쓴다.
     * message는 사람이 읽는 문구라 언제든 바뀔 수 있으므로 분기 기준이 될 수 없다.
     */
    default String getCode() {
        return this instanceof Enum<?> constant ? constant.name() : getClass().getSimpleName();
    }
}
