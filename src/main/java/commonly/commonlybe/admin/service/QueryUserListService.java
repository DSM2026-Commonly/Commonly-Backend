package commonly.commonlybe.admin.service;

import commonly.commonlybe.admin.entity.AdminRole;
import commonly.commonlybe.admin.repository.AdminRepository;
import commonly.commonlybe.admin.controller.dto.UserListResponse;
import lombok.RequiredArgsConstructor;
import commonly.commonlybe.global.page.PageNumber;
import commonly.commonlybe.global.page.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class QueryUserListService {
    private final AdminRepository adminRepository;

    @Transactional(readOnly = true)
    public PageResponse<UserListResponse> execute(int page, int size, String keyword) {
        Pageable pageable = PageNumber.toPageable(page, size);

        return PageResponse.of(
            adminRepository.findAllByRoleAndUser_NameContaining(
                AdminRole.USER, keyword == null ? "" : keyword, pageable),
            admin -> UserListResponse.builder()
                .userId(admin.getUser().getId())
                .accountId(admin.getUser().getAccountId())
                .name(admin.getUser().getName())
                .department(admin.getDepartment())
                .build());
    }
}
