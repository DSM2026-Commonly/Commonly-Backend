package commonly.commonlybe.domain.auth.presentation.dto.response;

import lombok.Builder;

@Builder
public record TokenResponse(
    String accessToken
) {
}
