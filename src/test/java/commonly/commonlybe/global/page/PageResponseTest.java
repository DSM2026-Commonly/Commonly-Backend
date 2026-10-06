package commonly.commonlybe.global.page;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void 외부로는_1부터_세는_페이지_번호를_내려준다() {
        // Spring Data 인덱스 0 == 첫 페이지
        PageResponse<String> response = PageResponse.from(
                new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 2), 5));

        assertThat(response.page()).isEqualTo(1);
    }

    @Test
    void 마지막_페이지가_정확히_size일_때도_hasNext가_false다() {
        // 배열만 반환하던 기존 응답으로는 구분할 수 없던 경우다
        PageResponse<String> response = PageResponse.from(
                new PageImpl<>(List.of("c", "d"), PageRequest.of(1, 2), 4));

        assertThat(response.content()).hasSize(2);
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    void 중간_페이지는_hasNext가_true다() {
        PageResponse<String> response = PageResponse.from(
                new PageImpl<>(List.of("c", "d"), PageRequest.of(1, 2), 7));

        assertThat(response.hasNext()).isTrue();
        assertThat(response.totalElements()).isEqualTo(7);
        assertThat(response.totalPages()).isEqualTo(4);
    }

    @Test
    void 매퍼로_엔티티를_DTO로_바꿔_담는다() {
        PageResponse<Integer> response = PageResponse.of(
                new PageImpl<>(List.of("a", "bb"), PageRequest.of(0, 2), 2),
                String::length);

        assertThat(response.content()).containsExactly(1, 2);
    }

    @Test
    void 페이지_번호_1이_인덱스_0으로_변환된다() {
        Pageable pageable = PageNumber.toPageable(1, 20);

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
    }

    @Test
    void 페이지_번호가_0이나_음수로_와도_첫_페이지로_떨어진다() {
        assertThat(PageNumber.toPageable(0, 10).getPageNumber()).isZero();
        assertThat(PageNumber.toPageable(-5, 10).getPageNumber()).isZero();
    }
}
