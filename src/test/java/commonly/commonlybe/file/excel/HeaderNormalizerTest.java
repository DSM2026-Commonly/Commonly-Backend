package commonly.commonlybe.file.excel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.Normalizer;
import org.junit.jupiter.api.Test;

class HeaderNormalizerTest {

    @Test
    void 개행과_앞_공백을_제거한다() {
        assertEquals("만료예정일", HeaderNormalizer.normalize("만료\n 예정일"));
    }

    @Test
    void 괄호_주석을_제거한다() {
        assertEquals("구분", HeaderNormalizer.normalize("구분\n(채용,전보,해지, 퇴직)"));
    }

    @Test
    void 근무형태_괄호_주석을_제거한다() {
        assertEquals("근무형태", HeaderNormalizer.normalize("근무형태\n(기간제,단시간근로자)"));
    }

    @Test
    void NFD로_분해된_한글을_NFC로_정규화한다() {
        String nfd = Normalizer.normalize("채용", Normalizer.Form.NFD);
        assertEquals("채용", HeaderNormalizer.normalize(nfd));
    }
}
