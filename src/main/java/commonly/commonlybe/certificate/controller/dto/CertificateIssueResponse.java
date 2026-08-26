package commonly.commonlybe.certificate.controller.dto;

public record CertificateIssueResponse(
        Long certificateId,
        String documentNo,
        String downloadUrl
) {
}
