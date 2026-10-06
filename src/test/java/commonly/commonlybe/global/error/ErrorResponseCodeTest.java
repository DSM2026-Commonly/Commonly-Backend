package commonly.commonlybe.global.error;

import commonly.commonlybe.certificate.exception.CertificateErrorCode;
import commonly.commonlybe.global.error.response.ErrorResponse;
import commonly.commonlybe.global.error.response.ValidationErrorResponse;
import commonly.commonlybe.user.exception.UserErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseCodeTest {

    @Test
    void 도메인_에러는_enum_이름을_code로_내려준다() {
        ErrorResponse response = ErrorResponse.from(UserErrorCode.USER_NOT_FOUND);

        assertThat(response.code()).isEqualTo("USER_NOT_FOUND");
        assertThat(response.status()).isEqualTo(404);
    }

    @Test
    void 같은_404라도_원인이_다르면_code가_다르다() {
        // FE가 message 문자열을 파싱하지 않고 분기할 수 있어야 한다
        ErrorResponse human = ErrorResponse.from(CertificateErrorCode.PETITIONER_HUMAN_NOT_MATCHED);
        ErrorResponse certificate = ErrorResponse.from(CertificateErrorCode.CERTIFICATE_NOT_FOUND);

        assertThat(human.status()).isEqualTo(certificate.status());
        assertThat(human.code()).isNotEqualTo(certificate.code());
    }

    @Test
    void ErrorProperty가_없는_경로는_상태코드_이름을_code로_쓴다() {
        ErrorResponse response = ErrorResponse.of(new IllegalStateException("무언가"), 405);

        assertThat(response.code()).isEqualTo("METHOD_NOT_ALLOWED");
    }

    @Test
    void enum에_없는_상태코드는_예외를_던지지_않고_HTTP_접두사를_붙인다() {
        // ResponseStatusException은 raw 상태코드로도 만들 수 있다.
        // valueOf()를 쓰면 에러 응답을 만드는 도중에 터진다.
        ErrorResponse response = ErrorResponse.of(new IllegalStateException("무언가"), 599);

        assertThat(response.code()).isEqualTo("HTTP_599");
        assertThat(response.status()).isEqualTo(599);
    }

    @Test
    void 검증_에러는_VALIDATION_FAILED를_code로_쓴다() {
        BindException exception = new BindException(
                new BeanPropertyBindingResult(new Object(), "target"));
        exception.rejectValue(null, "x", "틀렸습니다");

        ValidationErrorResponse response = ValidationErrorResponse.of(exception);

        assertThat(response.code()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.status()).isEqualTo(400);
    }
}
