package commonly.commonlybe.global.page;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * 페이지 응답 공통 래퍼.
 *
 * page는 **1부터 세는 번호**다. Spring Data의 Page.getNumber()는 0부터라 여기서 +1 한다.
 * 목록 API가 최상위 배열을 반환하면 클라이언트가 다음 페이지 유무를 추정해야 한다
 * (받은 개수 == size면 다음이 있다고 보는 식). 마지막 페이지가 정확히 size일 때 틀린다.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext());
    }

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return from(page.map(mapper));
    }
}
