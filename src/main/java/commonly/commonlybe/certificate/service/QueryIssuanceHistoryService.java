package commonly.commonlybe.certificate.service;

import commonly.commonlybe.certificate.controller.dto.IssuanceHistoryResponse;
import commonly.commonlybe.certificate.repository.CertificateIssuedRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
    public List<IssuanceHistoryResponse> execute(int page, int size, LocalDate startDate,
                                                 LocalDate endDate, String keyword) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);
        LocalDateTime start = (startDate == null ? MIN_DATE : startDate).atStartOfDay();
        LocalDateTime end = (endDate == null ? MAX_DATE : endDate.plusDays(1)).atStartOfDay();

        return certificateIssuedRepository
            .searchHistories(keyword == null ? "" : keyword, start, end, pageable)
            .getContent();
    }
}
