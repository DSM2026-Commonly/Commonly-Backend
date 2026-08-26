package commonly.commonlybe.certificate.controller.dto;

import commonly.commonlybe.human.entity.Gender;
import commonly.commonlybe.human.entity.HumanEntity;
import java.time.LocalDate;

public record CertificateHumanDto(
        Long humanId,
        String name,
        LocalDate birthDate,
        Gender gender,
        String address
) {
    public static CertificateHumanDto from(HumanEntity human) {
        return new CertificateHumanDto(human.getHumanId(), human.getName(),
                human.getBirthDate(), human.getGender(), human.getAddress());
    }
}
