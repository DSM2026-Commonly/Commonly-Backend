package commonly.commonlybe.certificate.controller.dto;

import java.time.LocalDateTime;

public record IssuanceHistoryResponse(
        Long issuanceHistoryId,
        String documentNo,
        Long humanId,
        String targetName,
        String purpose,
        int totalMonths,
        int totalDays,
        LocalDateTime issuedAt
) {
}
