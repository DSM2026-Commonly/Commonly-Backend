package commonly.commonlybe.certificate.controller.dto;

import java.time.LocalDateTime;

public record IssuanceHistoryResponse(
        Long issuanceHistoryId,
        String documentNo,
        Long humanId,
        String targetName,
        String purpose,
        /** 발급 사유. #46 이전 발급 건은 null이다. purpose(용도)와 다른 값이다. */
        String issueReason,
        int totalMonths,
        int totalDays,
        LocalDateTime issuedAt
) {
}
