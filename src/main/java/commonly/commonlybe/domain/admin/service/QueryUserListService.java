package commonly.commonlybe.domain.admin.service;

import commonly.commonlybe.domain.admin.domain.repository.AdminRepository;
import commonly.commonlybe.domain.admin.presentation.dto.response.UserListResponse;
import commonly.commonlybe.domain.user.domain.Authority;
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

        return adminRepository.findAllByUser_AuthorityAndUser_NameContaining(
                Authority.USER, keyword == null ? "" : keyword, pageable)
            .map(admin -> UserListResponse.builder()
                .userId(admin.getUser().getId())
                .accountId(admin.getUser().getAccountId())
                .name(admin.getUser().getName())
                .department(admin.getDepartment())
                .build())
            .getContent();
    }
}
