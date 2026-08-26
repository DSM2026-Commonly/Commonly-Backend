package commonly.commonlybe.human.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

/**
 * 인적사항 성별. API는 M/F 코드로 주고받고, DB에는 EnumType.STRING으로 MALE/FEMALE이 저장된다.
 */
@AllArgsConstructor
public enum Gender {
    MALE("M"),
    FEMALE("F");

    private final String code;

    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 알 수 없는 코드는 null을 반환해 @NotNull 검증이 400으로 처리하게 둔다.
     */
    @JsonCreator
    public static Gender fromCode(String code) {
        for (Gender gender : values()) {
            if (gender.code.equals(code)) {
                return gender;
            }
        }
        return null;
    }
}
