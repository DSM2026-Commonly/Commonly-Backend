package commonly.commonlybe.certificate.document;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import commonly.commonlybe.certificate.entity.CertificateEntity;
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
 * 「경력증명서 서식」(hwpx)과 같은 레이아웃의 XHTML을 만들어 PDF로 렌더한다 (certificate-domain.md §4).
 *
 * openhtmltopdf는 XHTML만 받는다. 태그를 하나라도 안 닫으면 파싱 에러가 나니 손댈 때 주의.
 * 한글 폰트를 임베드하지 않으면 글자가 전부 두부(□)로 나온다.
 *
 * ponytail: 직인 이미지가 아직 없어 "(인)" 글자만 찍는다 (§7-1 2번). 이미지가 생기면 footer에 img를 얹는다.
 */
@Slf4j
@Component
public class CertificatePdfRenderer {

    /** 서식 재직사항 표가 10행 고정이다. 모자라면 빈 행으로 채운다. */
    private static final int WORK_ROWS = 10;

    private static final String FONT_FAMILY = "NanumGothic";
    private static final String FONT_REGULAR = "/fonts/NanumGothic-Regular.ttf";
    private static final String FONT_BOLD = "/fonts/NanumGothic-Bold.ttf";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd.");
    private static final DateTimeFormatter ISSUE_DATE = DateTimeFormatter.ofPattern("yyyy.  M.  d.");

    private static final String STYLE = """
            @page { size: A4; margin: 18mm 16mm; }
            body { font-family: '%s'; font-size: 10.5pt; color: #000; }
            table { width: 100%%; border-collapse: collapse; }
            h1 { text-align: center; font-size: 22pt; letter-spacing: 12pt; text-decoration: underline; margin: 6px 0 22px; }
            .header td { padding: 2px 0; font-size: 10pt; }
            .header .staff { width: 30%%; }
            .main td { border: 1px solid #000; padding: 6px 4px; text-align: center; word-wrap: break-word; }
            .main .label { white-space: nowrap; }
            .main .date { white-space: nowrap; }
            .main .left { text-align: left; }
            .main .total { font-weight: bold; }
            .main .work td { height: 24px; }
            .footer { margin-top: 14px; }
            .footer p { margin: 10px 0; }
            .footer .statement { font-size: 12pt; }
            .footer .issued { text-align: center; margin-top: 24px; font-size: 12pt; }
            .footer .issuer { text-align: right; font-size: 13pt; margin: 22px 30px 0 0; }
            """.formatted(FONT_FAMILY);

    public byte[] render(CertificateDocument document) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFont(() -> font(FONT_REGULAR), FONT_FAMILY, 400, FontStyle.NORMAL, true);
            builder.useFont(() -> font(FONT_BOLD), FONT_FAMILY, 700, FontStyle.NORMAL, true);
            builder.withHtmlContent(toXhtml(document), null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException | RuntimeException e) {
            log.error("증명서 PDF 렌더 실패 : documentNo={}", document.documentNo(), e);
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_RENDER_FAILED);
        }
    }

    String toXhtml(CertificateDocument document) {
        HumanEntity human = document.human();
        StringBuilder html = new StringBuilder(8_192);
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>")
                .append(STYLE)
                .append("</style></head><body>");

        html.append("<h1>경력증명서</h1>");

        // 담당자/연락처는 발급 주체를 저장하지 않아 공란이다 (§7-1 1번).
        html.append("<table class=\"header\"><tr><td></td>")
                .append("<td class=\"staff\">담 당 자 :</td></tr><tr>")
                .append("<td>제 ").append(escape(document.documentNo())).append(" 호</td>")
                .append("<td class=\"staff\">연 락 처 :</td></tr></table>");

        // 서식 열 5개: 구분 | 부터(성명) | 까지(한글/영문) | 근무부서(생년월일) | 담당업무(생년월일 값)
        html.append("<table class=\"main\">")
                .append("<colgroup><col style=\"width:13%\"/><col style=\"width:15%\"/><col style=\"width:16%\"/>")
                .append("<col style=\"width:15%\"/><col style=\"width:41%\"/></colgroup>");

        // 인적사항. 성명(영문)은 어디에도 없어 공란 (§7-1 4번).
        html.append("<tr><td class=\"label\" rowspan=\"3\">인적사항</td><td class=\"label\" rowspan=\"2\">성 명</td>")
                .append("<td class=\"left\">(한글) ").append(escape(human.getName())).append("</td>")
                .append("<td class=\"label\" rowspan=\"2\">생년월일</td>")
                .append("<td rowspan=\"2\">").append(date(human.getBirthDate())).append("</td></tr>");
        html.append("<tr><td class=\"left\">(영문) </td></tr>");
        html.append("<tr><td class=\"label\">주 소</td><td class=\"left\" colspan=\"3\">")
                .append(escape(human.getAddress())).append("</td></tr>");

        // 재직사항: 헤더 2행 + 데이터 10행.
        List<CertificateEntity> certificates = document.certificates();
        int rows = Math.max(WORK_ROWS, certificates.size());
        html.append("<tr><td class=\"label\" rowspan=\"").append(rows + 2).append("\">재직사항</td>")
                .append("<td class=\"label\" colspan=\"2\">근무기간</td>")
                .append("<td class=\"label\" rowspan=\"2\">근무부서</td>")
                .append("<td class=\"label\" rowspan=\"2\">담당업무</td></tr>")
                .append("<tr><td class=\"label\">부터</td><td class=\"label\">까지</td></tr>");

        for (int i = 0; i < rows; i++) {
            html.append("<tr class=\"work\">");
            if (i < certificates.size()) {
                CertificateEntity certificate = certificates.get(i);
                // 근무부서는 certificate.department만 쓴다. humans.department로 채우면 틀린 값이 찍힌다 (§1-2).
                html.append("<td class=\"date\">").append(date(certificate.getHireDate())).append("</td>")
                        .append("<td class=\"date\">").append(date(certificate.workEndDate())).append("</td>")
                        .append("<td>").append(escape(certificate.getDepartment())).append("</td>")
                        .append("<td>").append(escape(certificate.getKeyResponsibilities())).append("</td>");
            } else {
                html.append("<td></td><td></td><td></td><td></td>");
            }
            html.append("</tr>");
        }

        // 서식에 퇴직사유 칸이 하나뿐이라 마지막(가장 최근 입사) 이력의 사유만 찍는다. 목록은 hire_date 오름차순이다.
        String reason = certificates.isEmpty() ? null : certificates.get(certificates.size() - 1).getReason();
        html.append("<tr><td class=\"label\">총 근무<br/>기간</td><td class=\"total\" colspan=\"2\">총 ")
                .append(document.total().months()).append(" 개월 ")
                .append(document.total().days()).append(" 일</td>")
                .append("<td class=\"label\">퇴직사유</td><td>").append(escape(reason)).append("</td></tr>");
        html.append("<tr><td class=\"label\">그 밖의<br/>사항</td><td class=\"left\" colspan=\"4\">")
                .append(multiline(document.otherMatters())).append("</td></tr>");
        html.append("<tr><td class=\"label\">용 도</td><td class=\"left\" colspan=\"4\">")
                .append(escape(document.purpose())).append("</td></tr>");
        html.append("</table>");

        html.append("<div class=\"footer\"><p class=\"statement\">위와 같이 재직ㆍ경력을 증명합니다.</p>")
                .append("<p class=\"issued\">").append(ISSUE_DATE.format(document.issuedDate())).append("</p>")
                .append("<p class=\"issuer\">대전광역시 유성구청장 (인)</p></div>");

        html.append("</body></html>");
        return html.toString();
    }

    private InputStream font(String path) {
        InputStream in = CertificatePdfRenderer.class.getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("폰트 파일이 없습니다: " + path);
        }
        return in;
    }

    private static String date(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    private static String multiline(String value) {
        return escape(value).replace("\r\n", "\n").replace("\n", "<br/>");
    }

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
                // XML 1.0에서 금지된 제어문자는 하나만 섞여도 파싱이 깨진다.
                default -> {
                    if (c >= 0x20 || c == '\n' || c == '\r' || c == '\t') {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }

    /** 서식 한 장에 찍히는 값 전부. 엔티티에서 그대로 꺼내 쓴다. */
    public record CertificateDocument(
            String documentNo,
            HumanEntity human,
            List<CertificateEntity> certificates,
            WorkPeriod total,
            String purpose,
            String otherMatters,
            LocalDate issuedDate) {
    }
}
