package commonly.commonlybe.domain.admin.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ChangeInitialPasswordRequest {

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;
}
