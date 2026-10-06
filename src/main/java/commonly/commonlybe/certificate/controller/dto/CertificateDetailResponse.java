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
        /** 발급 사유. 서식에는 찍히지 않고 발급 이력 확인용으로만 내려간다 (#46). 기존 발급 건은 null. */
        String issueReason,
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
                issued.getIssueReason(),
                human,
                issued.getTotalMonths(),
                issued.getTotalDays(),
                items);
    }
}
