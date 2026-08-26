package commonly.commonlybe.certificate.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 재직사항 표가 10행 고정이라 certificateIds는 10개까지다.
 * 넘치면 조용히 앞 10개만 찍는 대신 400으로 막는다.
 */
public record CertificateIssueRequest(
        @NotNull Long humanId,
        @NotEmpty @Size(max = 10) List<Long> certificateIds,
        @NotBlank @Size(max = 255) String purpose,
        @Size(max = 1000) String otherMatters
) {
}
