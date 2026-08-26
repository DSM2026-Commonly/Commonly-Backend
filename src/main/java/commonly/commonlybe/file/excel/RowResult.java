package commonly.commonlybe.file.excel;

import commonly.commonlybe.certificate.entity.CertificateEntity;

public sealed interface RowResult {

    record Success(CertificateEntity certificate) implements RowResult {
    }

    record Failure(String reason) implements RowResult {
    }

    record Skip() implements RowResult {
    }

    static RowResult success(CertificateEntity certificate) {
        return new Success(certificate);
    }

    static RowResult failure(String reason) {
        return new Failure(reason);
    }

    static RowResult skip() {
        return new Skip();
    }
}
