package commonly.commonlybe.global.page;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 외부 노출 페이지 번호(1부터)와 Spring Data 인덱스(0부터) 사이의 변환을 한 곳에 모은다.
 * 각 서비스가 제각기 `page - 1`을 하면 한 곳에서 빼먹는 순간 페이지가 하나씩 밀린다.
 */
public final class PageNumber {

    public static final int FIRST = 1;

    private PageNumber() {
    }

    public static Pageable toPageable(int page, int size) {
        return PageRequest.of(toIndex(page), size);
    }

    public static Pageable toPageable(int page, int size, Sort sort) {
        return PageRequest.of(toIndex(page), size, sort);
    }

    private static int toIndex(int page) {
        return Math.max(page - FIRST, 0);
    }
}
