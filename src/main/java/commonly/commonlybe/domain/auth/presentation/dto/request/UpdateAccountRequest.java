package commonly.commonlybe.domain.auth.presentation.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class UpdateAccountRequest {

    @Size(min = 1, max = 20)
    private String name;

    @Pattern(regexp = "^\\d{3}-\\d{3,4}-\\d{4}$")
    private String phoneNumber;

    private LocalDate birthDate;
}
