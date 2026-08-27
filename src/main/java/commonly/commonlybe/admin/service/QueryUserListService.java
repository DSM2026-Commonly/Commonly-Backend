package commonly.commonlybe.admin.service;

import commonly.commonlybe.admin.entity.AdminRole;
import commonly.commonlybe.admin.repository.AdminRepository;
import commonly.commonlybe.admin.controller.dto.UserListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QueryUserListService {
    private final AdminRepository adminRepository;

    @Transactional(readOnly = true)
    public List<UserListResponse> execute(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);

        return adminRepository.findAllByRoleAndUser_NameContaining(
                AdminRole.USER, keyword == null ? "" : keyword, pageable)
            .map(admin -> UserListResponse.builder()
                .userId(admin.getUser().getId())
                .accountId(admin.getUser().getAccountId())
                .name(admin.getUser().getName())
                .department(admin.getDepartment())
                .build())
            .getContent();
    }
}
