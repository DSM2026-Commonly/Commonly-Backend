package commonly.commonlybe.certificate.document;

import static org.assertj.core.api.Assertions.assertThat;

import commonly.commonlybe.certificate.entity.CertificateEntity;
import commonly.commonlybe.certificate.entity.CertificateIssuedEntity;
import commonly.commonlybe.human.entity.HumanEntity;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class CertificatePdfRendererTest {

    private final CertificatePdfRenderer renderer = new CertificatePdfRenderer();

    @Test
    void 서식_값을_채운_한_페이지짜리_PDF를_만든다() throws IOException {
        byte[] pdf = renderer.render(issued("은행 제출용"), human("홍길동"), List.of(
                certificate(LocalDate.of(2020, 3, 2), LocalDate.of(2021, 2, 28), "민원 안내", "계약만료"),
                certificate(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 1, 10), "자료 정리", "개인사정")));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            String text = new PDFTextStripper().getText(document);
            assertThat(text)
                    .contains("유성구-2026-000001")
                    .contains("홍길동")
                    .contains("1990. 1. 1.")
                    .contains("대전광역시 유성구 대학로 211")
                    .contains("2020. 3. 2.").contains("2021. 2. 28.")
                    .contains("민원 안내").contains("자료 정리")
                    .contains("총 12 개월 15 일")
                    .contains("개인사정")
                    .contains("은행 제출용")
                    .contains("2026. 10. 1.")
                    .contains("대전광역시 유성구청장");
        }
    }

    @Test
    void 값에_XML_특수문자가_섞여도_렌더된다() throws IOException {
        byte[] pdf = renderer.render(issued("A&B <제출>"), human("홍길동"), List.of(
                certificate(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 1, 10), "R&D \"지원\"", null)));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(new PDFTextStripper().getText(document))
                    .contains("A&B <제출>")
                    .contains("R&D \"지원\"");
        }
    }

    @Test
    void 재직사항이_10행이어도_한_페이지에_들어간다() throws IOException {
        List<CertificateEntity> certificates = java.util.stream.IntStream.range(0, 10)
                .mapToObj(i -> certificate(LocalDate.of(2010 + i, 1, 1), LocalDate.of(2010 + i, 12, 31),
                        "담당업무가 꽤 긴 경우를 가정한 설명 " + i, "계약만료"))
                .toList();

        try (PDDocument document = Loader.loadPDF(renderer.render(issued("제출용"), human("홍길동"), certificates))) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
        }
    }

    private CertificateIssuedEntity issued(String purpose) {
        return CertificateIssuedEntity.builder()
                .humanId(1L)
                .documentNo("유성구-2026-000001")
                .purpose(purpose)
                .otherMatters("없음")
                .totalMonths(12)
                .totalDays(15)
                .issuedAt(LocalDateTime.of(2026, 10, 1, 9, 30))
                .certificateIds(List.of(1L))
                .build();
    }

    private HumanEntity human(String name) {
        return HumanEntity.builder()
                .name(name)
                .birthDate(LocalDate.of(1990, 1, 1))
                .address("대전광역시 유성구 대학로 211")
                .build();
    }

    private CertificateEntity certificate(LocalDate hireDate, LocalDate retirementDate, String duties, String reason) {
        return CertificateEntity.builder()
                .humanId(1L)
                .name("홍길동")
                .hireDate(hireDate)
                .retirementDate(retirementDate)
                .keyResponsibilities(duties)
                .reason(reason)
                .build();
    }
}
