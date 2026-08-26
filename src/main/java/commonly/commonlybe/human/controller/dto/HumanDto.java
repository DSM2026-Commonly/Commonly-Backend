package commonly.commonlybe.human.controller.dto;

import commonly.commonlybe.human.entity.Gender;
import commonly.commonlybe.human.entity.HumanEntity;
import java.time.LocalDate;

public record HumanDto(
        Long humanId,
        String name,
        Gender gender,
        LocalDate birthDate,
        String address,
        String department
) {
    public static HumanDto from(HumanEntity human) {
        return new HumanDto(human.getHumanId(), human.getName(), human.getGender(),
                human.getBirthDate(), human.getAddress(), human.getDepartment());
    }
}
