package commonly.commonlybe.domain.auth.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ChangePasswordRequest {

    @NotBlank
    private String password;

    @NotBlank
    @Size(min = 8, max = 72)
    private String newPassword;
}
