package commonly.commonlybe.admin.controller.dto;

import lombok.Builder;

@Builder
public record UserListResponse(
    Long userId,
    String accountId,
    String name,
    String department
) {
}
