package commonly.commonlybe.certificate.controller.dto;

import commonly.commonlybe.certificate.entity.CertificateIssuedEntity;
import java.time.LocalDateTime;
import java.util.List;

public record CertificateDetailResponse(
        Long certificateId,
        String documentNo,
        LocalDateTime issuedAt,
        String purpose,
        String otherMatters,
        CertificateHumanDto human,
        int totalMonths,
        int totalDays,
        List<CertificateItemDto> items
) {
    public static CertificateDetailResponse of(CertificateIssuedEntity issued,
                                                CertificateHumanDto human,
                                                List<CertificateItemDto> items) {
        return new CertificateDetailResponse(
                issued.getCertificateIssuedId(),
                issued.getDocumentNo(),
                issued.getIssuedAt(),
                issued.getPurpose(),
                issued.getOtherMatters(),
                human,
                issued.getTotalMonths(),
                issued.getTotalDays(),
                items);
    }
}
