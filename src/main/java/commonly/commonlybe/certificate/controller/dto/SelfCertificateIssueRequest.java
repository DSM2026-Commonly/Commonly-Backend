package commonly.commonlybe.certificate.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 본인 발급. humanId는 인증 주체에서 나오므로 body에 없다.
 * certificateIds는 GET /api/certificates/self로 본 목록에서 고른 것. 비우면 전체.
 * 남의 id가 섞이면 CertificateIssueService가 humanId 조건으로 걸러 404를 낸다.
 */
public record SelfCertificateIssueRequest(
        @NotBlank @Size(max = 255) String purpose,
        @Size(max = 1000) String otherMatters,
        @Size(max = 10) List<Long> certificateIds
) {
}
