package commonly.commonlybe.domain.admin.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CreateUserRequest {

    @NotBlank
    @Pattern(regexp = "[a-zA-Z0-9]{4,12}")
    private String accountId;

    @NotBlank
    @Size(max = 20)
    private String name;

    @NotBlank
    @Size(max = 20)
    private String department;
}
