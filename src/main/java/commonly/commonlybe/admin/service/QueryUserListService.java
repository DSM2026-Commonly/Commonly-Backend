package commonly.commonlybe.admin.service;

import commonly.commonlybe.admin.entity.AdminRole;
import commonly.commonlybe.admin.repository.AdminRepository;
import commonly.commonlybe.admin.controller.dto.UserListResponse;
import lombok.RequiredArgsConstructor;
import commonly.commonlybe.global.page.PageNumber;
import commonly.commonlybe.global.page.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class QueryUserListService {

    /**
     * 정렬이 없으면 DB가 페이지마다 다른 순서를 줄 수 있어 경계 행이 중복되거나 누락된다.
     * 정렬 키를 Admin.id로 잡은 이유: @MapsId로 user_id와 같은 값이라 유일하고,
     * admin 테이블 자체 컬럼이라 User 조인을 타지 않는다.
     * (user.name 같은 키는 동명이인에서 다시 불안정해져 2차 키 없이는 쓸 수 없다.)
     */
    private static final Sort STABLE_SORT = Sort.by(Sort.Direction.ASC, "id");

    private final AdminRepository adminRepository;

    @Transactional(readOnly = true)
    public PageResponse<UserListResponse> execute(int page, int size, String keyword) {
        Pageable pageable = PageNumber.toPageable(page, size, STABLE_SORT);

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
