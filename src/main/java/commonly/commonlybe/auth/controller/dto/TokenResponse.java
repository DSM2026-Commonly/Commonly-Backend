package commonly.commonlybe.auth.controller.dto;

import lombok.Builder;

@Builder
public record TokenResponse(
    String accessToken
) {
}
