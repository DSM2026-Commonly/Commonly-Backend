package commonly.commonlybe.certificate.service;

import commonly.commonlybe.certificate.controller.dto.IssuanceHistoryResponse;
import commonly.commonlybe.certificate.repository.CertificateIssuedRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import commonly.commonlybe.global.page.PageNumber;
import commonly.commonlybe.global.page.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QueryIssuanceHistoryService {
    private static final LocalDate MIN_DATE = LocalDate.of(1970, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final CertificateIssuedRepository certificateIssuedRepository;

    @Transactional(readOnly = true)
    public PageResponse<IssuanceHistoryResponse> execute(int page, int size, LocalDate startDate,
                                                         LocalDate endDate, String keyword) {
        Pageable pageable = PageNumber.toPageable(page, size);
        LocalDateTime start = (startDate == null ? MIN_DATE : startDate).atStartOfDay();
        LocalDateTime end = (endDate == null ? MAX_DATE : endDate.plusDays(1)).atStartOfDay();

        return PageResponse.from(certificateIssuedRepository
            .searchHistories(keyword == null ? "" : keyword, start, end, pageable));
    }
}
