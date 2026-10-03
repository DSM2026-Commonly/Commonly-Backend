package commonly.commonlybe.certificate.document;

import static org.assertj.core.api.Assertions.assertThat;

import commonly.commonlybe.certificate.document.CertificatePdfRenderer.CertificateDocument;
import commonly.commonlybe.certificate.entity.CertificateEntity;
import commonly.commonlybe.human.entity.HumanEntity;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class CertificatePdfRendererTest {

    private final CertificatePdfRenderer renderer = new CertificatePdfRenderer();

    @Test
    void 서식_값이_한글로_찍힌_한_장짜리_PDF를_만든다() throws IOException {
        byte[] pdf = renderer.render(document("은행 제출용", "특이사항 없음"));

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        try (PDDocument loaded = Loader.loadPDF(pdf)) {
            assertThat(loaded.getNumberOfPages()).isEqualTo(1);
            // 폰트가 임베드 안 되면 텍스트 추출에서 한글이 빠진다. 제목은 자간 때문에 글자 사이가 벌어져 나온다.
            String text = new PDFTextStripper().getText(loaded);
            assertThat(text).contains("경 력 증 명 서", "유성구-2026-000001", "홍길동", "1990.01.01.",
                    "대전광역시 유성구", "2022.01.01.", "2022.01.10.", "민원 안내", "계약만료",
                    "총 0 개월 10 일", "은행 제출용", "특이사항 없음", "대전광역시 유성구청장");
        }
    }

    @Test
    void 입력값에_마크업이_섞여도_태그로_해석하지_않는다() throws IOException {
        byte[] pdf = renderer.render(document("<b>용도</b> & 기타", "줄1\n줄2\u0000"));

        try (PDDocument loaded = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(loaded);
            assertThat(text).contains("<b>용도</b> & 기타", "줄1", "줄2");
            assertThat(text).doesNotContain("<br/>");
        }
    }

    @Test
    void 재직사항_10행을_다_채워도_한_장에_들어간다() throws IOException {
        List<CertificateEntity> certificates = IntStream.range(0, 10)
                .mapToObj(i -> CertificateEntity.builder()
                        .name("홍길동")
                        .hireDate(LocalDate.of(2010 + i, 3, 1))
                        .retirementDate(LocalDate.of(2010 + i, 12, 31))
                        .department("자치행정과")
                        .keyResponsibilities("민원 안내 및 서류 접수 보조")
                        .reason("계약기간 만료")
                        .build())
                .toList();
        CertificateDocument base = document("은행 제출용", "특이사항 없음");
        byte[] pdf = renderer.render(new CertificateDocument(base.documentNo(), base.human(), certificates,
                new WorkPeriod(100, 0), base.purpose(), base.otherMatters(), base.issuedDate()));

        try (PDDocument loaded = Loader.loadPDF(pdf)) {
            assertThat(loaded.getNumberOfPages()).isEqualTo(1);
        }
    }

    private CertificateDocument document(String purpose, String otherMatters) {
        HumanEntity human = HumanEntity.builder()
                .name("홍길동")
                .birthDate(LocalDate.of(1990, 1, 1))
                .address("대전광역시 유성구 대학로 1")
                .build();
        CertificateEntity certificate = CertificateEntity.builder()
                .name("홍길동")
                .hireDate(LocalDate.of(2022, 1, 1))
                .retirementDate(LocalDate.of(2022, 1, 10))
                .keyResponsibilities("민원 안내")
                .reason("계약만료")
                .build();
        return new CertificateDocument("유성구-2026-000001", human, List.of(certificate),
                new WorkPeriod(0, 10), purpose, otherMatters, LocalDate.of(2026, 10, 1));
    }
}
