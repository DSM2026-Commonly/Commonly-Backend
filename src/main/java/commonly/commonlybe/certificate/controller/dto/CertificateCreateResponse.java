package commonly.commonlybe.certificate.controller.dto;

/** 등록된 재직 이력의 id. 발급(§5.1)의 certificateIds에 그대로 넣는 값이다. */
public record CertificateCreateResponse(Long certificateId) {
}
