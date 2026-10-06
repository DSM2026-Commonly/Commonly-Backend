package commonly.commonlybe.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import commonly.commonlybe.admin.entity.Admin;
import commonly.commonlybe.admin.entity.AdminRole;
import commonly.commonlybe.admin.repository.AdminRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class QueryUserListServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @InjectMocks
    private QueryUserListService queryUserListService;

    /**
     * H2/Postgres가 우연히 삽입 순서를 돌려주면 중복·누락은 테스트에서 안 보인다.
     * 그래서 "정렬을 실제로 넘겼는지"를 Pageable 수준에서 못 박아 둔다.
     * 프로퍼티 경로가 JPA에서 유효한지는 UserListPagingApiTest가 확인한다.
     */
    @Test
    void 정렬_없는_Pageable로는_조회하지_않는다() {
        given(adminRepository.findAllByRoleAndUser_NameContaining(
                any(AdminRole.class), anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.<Admin>of()));

        queryUserListService.execute(1, 10, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(adminRepository).findAllByRoleAndUser_NameContaining(
                any(AdminRole.class), anyString(), captor.capture());

        Pageable pageable = captor.getValue();
        assertThat(pageable.getSort().isSorted()).isTrue();
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Test
    void 페이지_번호는_1부터_세고_정렬은_페이지가_바뀌어도_유지된다() {
        given(adminRepository.findAllByRoleAndUser_NameContaining(
                any(AdminRole.class), anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.<Admin>of()));

        queryUserListService.execute(3, 5, "홍");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(adminRepository).findAllByRoleAndUser_NameContaining(
                any(AdminRole.class), anyString(), captor.capture());

        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort().isSorted()).isTrue();
    }
}
