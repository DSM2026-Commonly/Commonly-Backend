package commonly.commonlybe.certificate.exception;

import commonly.commonlybe.global.error.exception.CommonlyException;

public class CertificateException extends CommonlyException {

    public CertificateException(CertificateErrorCode certificateErrorCode) {
        super(certificateErrorCode);
    }

    public CertificateException(CertificateErrorCode certificateErrorCode, String message) {
        super(certificateErrorCode, message);
    }
}
