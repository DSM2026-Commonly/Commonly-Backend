package commonly.commonlybe.certificate.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 본인 발급. humanId와 certificateIds는 인증 주체에서 나오므로 body에 없다.
 */
public record SelfCertificateIssueRequest(
        @NotBlank @Size(max = 255) String purpose,
        @Size(max = 1000) String otherMatters
) {
}
