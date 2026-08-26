package commonly.commonlybe.certificate.controller;

import commonly.commonlybe.certificate.controller.dto.IssuanceHistoryResponse;
import commonly.commonlybe.certificate.service.QueryIssuanceHistoryService;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/issuance-histories")
@RequiredArgsConstructor
public class IssuanceHistoryController {
    private final QueryIssuanceHistoryService queryIssuanceHistoryService;

    @GetMapping
    public List<IssuanceHistoryResponse> queryIssuanceHistories(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword) {
        return queryIssuanceHistoryService.execute(page, size, startDate, endDate, keyword);
    }
}
