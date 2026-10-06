package commonly.commonlybe.file.excel;

import commonly.commonlybe.file.exception.FileException;
import commonly.commonlybe.global.error.error_code.FileErrorCode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * POI는 OOXML이 아닌 입력에 런타임 예외를 던지고, 그게 처리되지 않아 500으로 새어 나갔다 (#79).
 * 전부 FileException으로 바뀌어 4xx가 되어야 한다.
 */
class ExcelParserInvalidInputTest {

    private static final int MAX_ROWS = 10_000;

    /** HWPX도 ZIP 컨테이너라서 FileValidator의 매직넘버 검사를 통과한다. */
    private byte[] hwpxLikeZip() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("mimetype"));
            zip.write("application/hwp+zip".getBytes());
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("Contents/content.hpf"));
            zip.write("<?xml version=\"1.0\"?><package/>".getBytes());
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    @Test
    void 확장자만_xlsx인_비엑셀_ZIP은_NOT_AN_EXCEL_FILE이다() throws Exception {
        byte[] hwpx = hwpxLikeZip();

        // ZIP 매직넘버를 통과하는 입력이라는 전제를 고정한다
        assertThat(new byte[]{hwpx[0], hwpx[1], hwpx[2], hwpx[3]})
                .containsExactly(0x50, 0x4B, 0x03, 0x04);

        assertThatThrownBy(() -> ExcelParser.parse(new ByteArrayInputStream(hwpx), MAX_ROWS))
                .isInstanceOf(FileException.class)
                .hasFieldOrPropertyWithValue("errorProperty", FileErrorCode.NOT_AN_EXCEL_FILE);
    }

    @Test
    void 엑셀이_아닌_평범한_ZIP도_NOT_AN_EXCEL_FILE이다() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("hello.txt"));
            zip.write("hello".getBytes());
            zip.closeEntry();
        }

        assertThatThrownBy(() -> ExcelParser.parse(new ByteArrayInputStream(out.toByteArray()), MAX_ROWS))
                .isInstanceOf(FileException.class)
                .hasFieldOrPropertyWithValue("errorProperty", FileErrorCode.NOT_AN_EXCEL_FILE);
    }

    @Test
    void ZIP조차_아닌_입력도_NOT_AN_EXCEL_FILE이다() {
        byte[] garbage = "이건 그냥 텍스트입니다".getBytes();

        assertThatThrownBy(() -> ExcelParser.parse(new ByteArrayInputStream(garbage), MAX_ROWS))
                .isInstanceOf(FileException.class)
                .hasFieldOrPropertyWithValue("errorProperty", FileErrorCode.NOT_AN_EXCEL_FILE);
    }

    /**
     * 업로드 경로는 FileValidator가 먼저 걸러내지만,
     * MappingConfirmService가 저장된 파일을 다시 파싱할 때는 이 경로로 들어온다.
     */
    @Test
    void 빈_파일은_UNPROCESSABLE_FILE이다() {
        assertThatThrownBy(() -> ExcelParser.parse(new ByteArrayInputStream(new byte[0]), MAX_ROWS))
                .isInstanceOf(FileException.class)
                .hasFieldOrPropertyWithValue("errorProperty", FileErrorCode.UNPROCESSABLE_FILE);
    }

    @Test
    void 어떤_입력에도_처리되지_않은_런타임_예외가_새지_않는다() throws Exception {
        for (byte[] input : new byte[][]{
                hwpxLikeZip(), new byte[0], "x".getBytes(), new byte[]{0x50, 0x4B, 0x03, 0x04}}) {
            assertThatThrownBy(() -> ExcelParser.parse(new ByteArrayInputStream(input), MAX_ROWS))
                    .as("입력 %d바이트", input.length)
                    .isInstanceOf(FileException.class);
        }
    }
}
