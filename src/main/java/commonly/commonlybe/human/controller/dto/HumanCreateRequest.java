package commonly.commonlybe.human.controller.dto;

import commonly.commonlybe.human.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record HumanCreateRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull Gender gender,
        @NotNull @PastOrPresent LocalDate birthDate,
        @Size(max = 255) String address,
        @Size(max = 255) String department
) {
}
