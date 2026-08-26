package commonly.commonlybe.certificate.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Gender {
    MALE("남"),
    FEMALE("여");

    private final String label;

    public static Gender from(String label) {
        for (Gender gender : values()) {
            if (gender.label.equals(label)) {
                return gender;
            }
        }
        return null;
    }
}
