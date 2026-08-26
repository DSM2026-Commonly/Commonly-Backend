package commonly.commonlybe.human.controller.dto;

import commonly.commonlybe.human.entity.Gender;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

/**
 * 모든 조건은 nullable이며 null이면 해당 조건을 적용하지 않는다.
 * page/size는 명세에 없어 기본값(0, 20)으로 채운다.
 */
public record HumanSearchRequest(
        String name,
        Gender gender,
        LocalDate birthDateFrom,
        LocalDate birthDateTo,
        String address,
        @PositiveOrZero Integer page,
        @Positive @Max(100) Integer size
) {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    public HumanSearchRequest {
        // 빈 문자열이 "전부 일치"로 새지 않도록 null로 정규화한다.
        name = trimToNull(name);
        address = trimToNull(address);
        page = page == null ? DEFAULT_PAGE : page;
        size = size == null ? DEFAULT_SIZE : size;
    }

    @AssertTrue(message = "생년월일 시작일이 종료일보다 늦습니다.")
    public boolean isBirthDateRangeValid() {
        return birthDateFrom == null || birthDateTo == null || !birthDateFrom.isAfter(birthDateTo);
    }

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
