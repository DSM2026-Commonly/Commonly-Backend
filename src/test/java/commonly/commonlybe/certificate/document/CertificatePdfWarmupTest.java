package commonly.commonlybe.certificate.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import commonly.commonlybe.certificate.document.CertificatePdfRenderer.CertificateDocument;
import commonly.commonlybe.certificate.exception.CertificateErrorCode;
import commonly.commonlybe.certificate.exception.CertificateException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CertificatePdfWarmupTest {

    @Test
    void 웜업은_렌더러를_한_번_호출한다() {
        CertificatePdfRenderer renderer = mock(CertificatePdfRenderer.class);
        given(renderer.render(any())).willReturn("%PDF-1.4".getBytes(StandardCharsets.US_ASCII));

        new CertificatePdfWarmup(renderer).warmUp();

        ArgumentCaptor<CertificateDocument> captor = ArgumentCaptor.forClass(CertificateDocument.class);
        verify(renderer, times(1)).render(captor.capture());
        CertificateDocument document = captor.getValue();
        assertThat(document.human()).isNotNull();
        assertThat(document.certificates()).isNotEmpty();
        // DB를 타지 않고 메모리에서만 만들었는지. 저장된 적이 없으면 식별자가 비어 있다.
        assertThat(document.human().getHumanId()).isNull();
        assertThat(document.certificates().getFirst().getCertificateId()).isNull();
    }

    @Test
    void 실제_렌더러로_웜업하면_PDF가_만들어진다() {
        CertificatePdfRenderer renderer = spy(new CertificatePdfRenderer());
        AtomicReference<byte[]> rendered = new AtomicReference<>();
        doAnswer(invocation -> {
            byte[] pdf = (byte[]) invocation.callRealMethod();
            rendered.set(pdf);
            return pdf;
        }).when(renderer).render(any());

        new CertificatePdfWarmup(renderer).warmUp();

        assertThat(rendered.get()).isNotNull();
        assertThat(new String(rendered.get(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void 렌더러가_예외를_던져도_웜업은_예외를_밖으로_내보내지_않는다() {
        CertificatePdfRenderer renderer = mock(CertificatePdfRenderer.class);
        doThrow(new CertificateException(CertificateErrorCode.CERTIFICATE_RENDER_FAILED))
                .when(renderer).render(any());

        // 기동을 막아선 안 된다. ApplicationReadyEvent 리스너에서 예외가 새면 부팅이 실패한다.
        assertThatCode(() -> new CertificatePdfWarmup(renderer).warmUp()).doesNotThrowAnyException();
    }

    @Test
    void 렌더러가_예상못한_런타임예외를_던져도_웜업은_조용히_넘어간다() {
        CertificatePdfRenderer renderer = mock(CertificatePdfRenderer.class);
        // CertificateException으로 감싸이기 전에 터지는 경로도 있다 (예: 폰트 리소스 누락).
        doThrow(new IllegalStateException("폰트 파일이 없습니다")).when(renderer).render(any());

        assertThatCode(() -> new CertificatePdfWarmup(renderer).warmUp()).doesNotThrowAnyException();
    }
}
