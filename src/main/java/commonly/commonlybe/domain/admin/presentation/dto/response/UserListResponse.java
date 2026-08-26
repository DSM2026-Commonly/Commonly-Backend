package commonly.commonlybe.domain.admin.presentation.dto.response;

import lombok.Builder;

@Builder
public record UserListResponse(
    Long userId,
    String accountId,
    String name,
    String department
) {
}
