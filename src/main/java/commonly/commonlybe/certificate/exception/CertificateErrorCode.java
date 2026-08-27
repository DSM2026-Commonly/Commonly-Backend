package commonly.commonlybe.certificate.exception;

import commonly.commonlybe.global.error.error_code.ErrorProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum CertificateErrorCode implements ErrorProperty {
    CERTIFICATE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 경력사항을 찾을 수 없습니다."),
    CERTIFICATE_ISSUED_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 경력증명서를 찾을 수 없습니다."),
    CERTIFICATE_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "발급된 증명서 파일이 없습니다."),
    CERTIFICATE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST,
            "재직 이력이 10건을 넘어 본인 발급이 불가능합니다. 민원 담당자에게 문의하세요."),
    PETITIONER_HUMAN_NOT_MATCHED(HttpStatus.NOT_FOUND,
            "계정 정보와 일치하는 인적사항이 없습니다."),
    NOT_OWN_CERTIFICATE(HttpStatus.FORBIDDEN, "본인의 경력증명서만 조회할 수 있습니다."),
    SELF_ISSUE_DISABLED(HttpStatus.FORBIDDEN, "본인 발급은 현재 사용할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
