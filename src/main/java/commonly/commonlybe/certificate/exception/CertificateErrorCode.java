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
    CERTIFICATE_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "발급된 증명서 파일이 없습니다.");

    private final HttpStatus status;
    private final String message;
}
