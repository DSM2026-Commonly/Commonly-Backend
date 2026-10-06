package commonly.commonlybe.certificate.document;

import commonly.commonlybe.certificate.document.CertificatePdfRenderer.CertificateDocument;
import commonly.commonlybe.certificate.entity.CertificateEntity;
import commonly.commonlybe.human.entity.HumanEntity;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 기동 직후 더미 증명서를 한 번 렌더해서 콜드 스타트 비용을 부팅 시간으로 옮긴다 (issue #61).
 *
 * 실측으로 첫 렌더만 704ms, 이후는 67~89ms였다. PDFBox/openhtmltopdf 클래스 로딩,
 * 2.1MB TTF 2개 파싱, JIT 컴파일이 첫 요청 한 번에 몰리는 탓이다. FE가 본 미리보기 7.9초가
 * S3를 타는 발급(5.0초)보다 오히려 느렸던 것도 미리보기가 배포 후 첫 PDF 요청이었기 때문이다.
 * 그 한 번을 아무도 기다리지 않는 기동 시점에 미리 치른다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CertificatePdfWarmup {

    private final CertificatePdfRenderer renderer;

    /**
     * 웜업은 성능 최적화일 뿐이라 실패해도 기동을 막아선 안 된다.
     * 폰트가 빠졌든 렌더가 깨졌든 실제 발급 경로가 같은 이유로 실패할 뿐이고,
     * 그건 발급 요청에서 CERTIFICATE_RENDER_FAILED로 드러나야 할 문제다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        try {
            long startedAt = System.nanoTime();
            byte[] pdf = renderer.render(dummyDocument());
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("증명서 PDF 렌더러 웜업 완료 : {}ms, {}바이트", elapsedMs, pdf.length);
        } catch (Exception e) {
            log.warn("증명서 PDF 렌더러 웜업 실패 - 첫 발급 요청이 느릴 수 있다", e);
        }
    }

    /** DB를 타지 않도록 메모리에서만 만든다. 저장하지 않으니 식별자도 필요 없다. */
    private CertificateDocument dummyDocument() {
        HumanEntity human = HumanEntity.builder()
                .name("웜업")
                .birthDate(LocalDate.of(1990, 1, 1))
                .address("대전광역시 유성구")
                .build();
        CertificateEntity certificate = CertificateEntity.builder()
                .name("웜업")
                .hireDate(LocalDate.of(2024, 1, 1))
                .retirementDate(LocalDate.of(2024, 12, 31))
                .department("웜업과")
                .keyResponsibilities("웜업")
                .reason("웜업")
                .build();
        return new CertificateDocument("WARMUP-0000000", human, List.of(certificate),
                new WorkPeriod(12, 0), "웜업", "웜업", LocalDate.now());
    }
}
