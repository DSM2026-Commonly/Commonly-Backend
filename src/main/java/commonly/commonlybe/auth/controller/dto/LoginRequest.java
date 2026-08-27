package commonly.commonlybe.auth.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequest {

    @NotBlank
    private String accountId;

    @NotBlank
    private String password;
}
