package commonly.commonlybe.certificate.document;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import commonly.commonlybe.certificate.entity.CertificateEntity;
import commonly.commonlybe.certificate.entity.CertificateIssuedEntity;
import commonly.commonlybe.certificate.exception.CertificateErrorCode;
import commonly.commonlybe.certificate.exception.CertificateException;
import commonly.commonlybe.human.entity.HumanEntity;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 유성구청 「경력증명서 서식」(hwpx)과 같은 레이아웃의 XHTML을 만들어 PDF로 렌더한다.
 * hwpx는 서버에서 채울 방법이 없어 레이아웃 명세로만 쓴다 (docs/certificate-domain.md §4).
 *
 * 열 너비는 서식의 HWP 셀 너비 비율, 행 높이는 HWP 단위(1mm = 283.46)를 mm로 옮긴 값이다.
 */
@Slf4j
@Component
public class CertificatePdfRenderer {

    /** 서식 재직사항 표가 10행 고정이다. 모자라면 빈 행으로 채운다. */
    private static final int WORK_ROWS = 10;

    private static final String FONT_FAMILY = "NanumGothic";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy. M. d.");

    private static final String STYLE = """
            @page { size: A4; margin: 15mm 20mm 10mm 17mm; }
            body { font-family: 'NanumGothic'; font-size: 10.5pt; margin: 0; }
            h1 { text-align: center; font-size: 22pt; font-weight: bold; letter-spacing: 6pt; margin: 4mm 0 8mm; }
            table { width: 100%; border-collapse: collapse; table-layout: fixed; }
            .head td { padding: 0 1mm 1mm; vertical-align: bottom; }
            .head .contact { text-align: left; line-height: 1.6; }
            .form td { border: 0.4pt solid #000; padding: 0.5mm 1.5mm; text-align: center; vertical-align: middle;
                       word-wrap: break-word; }
            .form { border: 1.2pt solid #000; }
            .form .label { font-weight: bold; }
            .form .left { text-align: left; }
            .form .side { font-size: 9.5pt; padding: 0.5mm 0; white-space: nowrap; }
            .h-small { height: 6.8mm; }
            .h-row { height: 9.9mm; }
            .h-addr { height: 12mm; }
            .h-other { height: 10.6mm; }
            .h-purpose { height: 9.8mm; }
            .statement { text-align: center; font-size: 12pt; margin: 12mm 0 10mm; }
            .date { text-align: center; font-size: 12pt; margin-bottom: 16mm; }
            .issuer { text-align: center; font-size: 16pt; font-weight: bold; letter-spacing: 2pt; }
            """;

    public byte[] render(CertificateIssuedEntity issued, HumanEntity human, List<CertificateEntity> certificates) {
        String xhtml = toXhtml(issued, human, certificates);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFont(() -> font("NanumGothic-Regular.ttf"), FONT_FAMILY, 400, FontStyle.NORMAL, true);
            builder.useFont(() -> font("NanumGothic-Bold.ttf"), FONT_FAMILY, 700, FontStyle.NORMAL, true);
            builder.withHtmlContent(xhtml, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException | RuntimeException e) {
            log.error("증명서 PDF 렌더 실패 : documentNo={}", issued.getDocumentNo(), e);
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_PDF_RENDER_FAILED);
        }
    }

    private InputStream font(String fileName) {
        InputStream in = getClass().getResourceAsStream("/fonts/" + fileName);
        if (in == null) {
            // 폰트 없이 렌더하면 한글이 전부 두부(□)로 찍힌 PDF가 정상 발급된다. 그보다 실패가 낫다.
            throw new IllegalStateException("폰트 리소스 없음: " + fileName);
        }
        return in;
    }

    String toXhtml(CertificateIssuedEntity issued, HumanEntity human, List<CertificateEntity> certificates) {
        if (certificates.size() > WORK_ROWS) {
            // 표를 넘겨 2페이지로 흘리거나 앞 10개만 찍는 건 증명서에서 하면 안 된다. 발급 단계에서 막혀야 정상.
            throw new IllegalArgumentException("재직사항은 최대 %d행이다: %d".formatted(WORK_ROWS, certificates.size()));
        }

        StringBuilder html = new StringBuilder(8192);
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>")
                .append(STYLE)
                .append("</style></head><body>");

        html.append("<h1>경 력 증 명 서</h1>");

        // 머리말: 문서번호 / 담당자·연락처. 발급 주체를 저장하지 않아 담당자 칸은 공란 (§7-1 1번).
        html.append("<table class=\"head\"><colgroup><col style=\"width:69%\"/><col style=\"width:31%\"/></colgroup><tr>")
                .append("<td>제 ").append(escape(issued.getDocumentNo())).append("</td>")
                .append("<td class=\"contact\">담 당 자 :<br/>연 락 처 :</td>")
                .append("</tr></table>");

        html.append("<table class=\"form\"><colgroup>")
                .append(col(12.34)).append(col(13.23)).append(col(2.27)).append(col(16.07))
                .append(col(15.40)).append(col(40.69))
                .append("</colgroup>");

        // 인적사항
        html.append("<tr class=\"h-small\">")
                .append("<td class=\"label side\" rowspan=\"3\">인적사항</td>")
                .append("<td class=\"label\" rowspan=\"2\">성 명</td>")
                .append("<td class=\"left\" colspan=\"2\">(한글) ").append(escape(human.getName())).append("</td>")
                .append("<td class=\"label\" rowspan=\"2\">생년월일</td>")
                .append("<td rowspan=\"2\">").append(date(human.getBirthDate())).append("</td>")
                .append("</tr>");
        // 성명(영문)은 원천 데이터가 없다. 공란 (§7-1 4번).
        html.append("<tr class=\"h-small\"><td class=\"left\" colspan=\"2\">(영문)</td></tr>");
        html.append("<tr class=\"h-addr\">")
                .append("<td class=\"label\">주 소</td>")
                .append("<td class=\"left\" colspan=\"4\">").append(escape(human.getAddress())).append("</td>")
                .append("</tr>");

        // 재직사항 헤더
        html.append("<tr class=\"h-small\">")
                .append("<td class=\"label side\">재직사항</td>")
                .append("<td class=\"label\" colspan=\"3\">근무기간</td>")
                .append("<td class=\"label\" rowspan=\"2\">근무부서</td>")
                .append("<td class=\"label\" rowspan=\"2\">담당업무</td>")
                .append("</tr>");
        html.append("<tr class=\"h-small\">")
                .append("<td rowspan=\"").append(WORK_ROWS + 1).append("\"></td>")
                .append("<td class=\"label\" colspan=\"2\">부터</td>")
                .append("<td class=\"label\">까지</td>")
                .append("</tr>");

        // 재직사항 10행. 근무부서는 데이터 원천이 없어 비어 있을 수 있다 (§1-2).
        for (int i = 0; i < WORK_ROWS; i++) {
            html.append("<tr class=\"h-row\">");
            if (i < certificates.size()) {
                CertificateEntity certificate = certificates.get(i);
                html.append("<td colspan=\"2\">").append(date(certificate.getHireDate())).append("</td>")
                        .append("<td>").append(date(certificate.workEndDate())).append("</td>")
                        .append("<td>").append(escape(certificate.getDepartment())).append("</td>")
                        .append("<td class=\"left\">").append(escape(certificate.getKeyResponsibilities()))
                        .append("</td>");
            } else {
                html.append("<td colspan=\"2\"></td><td></td><td></td><td></td>");
            }
            html.append("</tr>");
        }

        html.append("<tr class=\"h-addr\">")
                .append("<td class=\"label side\">총 근무기간</td>")
                .append("<td colspan=\"3\">총 ").append(issued.getTotalMonths()).append(" 개월 ")
                .append(issued.getTotalDays()).append(" 일</td>")
                .append("<td class=\"label\">퇴직사유</td>")
                .append("<td class=\"left\">").append(escape(retirementReason(certificates))).append("</td>")
                .append("</tr>");
        html.append("<tr class=\"h-other\">")
                .append("<td class=\"label side\">그 밖의 사항</td>")
                .append("<td class=\"left\" colspan=\"5\">").append(escape(issued.getOtherMatters())).append("</td>")
                .append("</tr>");
        html.append("<tr class=\"h-purpose\">")
                .append("<td class=\"label side\">용도</td>")
                .append("<td class=\"left\" colspan=\"5\">").append(escape(issued.getPurpose())).append("</td>")
                .append("</tr>");
        html.append("</table>");

        html.append("<p class=\"statement\">위와 같이 재직ㆍ경력을 증명합니다.</p>");
        html.append("<p class=\"date\">").append(date(issued.getIssuedAt().toLocalDate())).append("</p>");
        // 직인 이미지는 아직 없다 (§7-1 2번). 확보되면 "(인)" 자리에 겹쳐 찍는다.
        html.append("<p class=\"issuer\">대전광역시 유성구청장 (인)</p>");

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * 서식의 퇴직사유 칸은 하나뿐이다. 재직사항은 입사일 오름차순이므로 마지막 행이 가장 최근 재직이다.
     */
    private String retirementReason(List<CertificateEntity> certificates) {
        return certificates.isEmpty() ? null : certificates.getLast().getReason();
    }

    private static String col(double percent) {
        return "<col style=\"width:" + percent + "%\"/>";
    }

    private static String date(LocalDate date) {
        return date == null ? "" : DATE_FORMATTER.format(date);
    }

    /** openhtmltopdf는 XHTML만 받는다. 값에 &lt; &amp; 가 섞이면 파싱 에러로 발급이 실패한다. */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&#39;");
                default -> escaped.append(c);
            }
        }
        return escaped.toString();
    }
}
