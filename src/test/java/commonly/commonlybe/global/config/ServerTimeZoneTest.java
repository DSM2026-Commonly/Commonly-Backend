package commonly.commonlybe.global.config;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ServerTimeZoneTest {

    @Test
    void JVM_기본_시간대가_한국으로_고정된다() {
        assertThat(TimeZone.getDefault().toZoneId()).isEqualTo(ZoneId.of("Asia/Seoul"));
        assertThat(ZoneId.systemDefault()).isEqualTo(ZoneId.of("Asia/Seoul"));
    }

    /**
     * TimeZone.setDefault()로 기본값만 바꾸면 user.timezone 프로퍼티는 UTC로 남는다.
     * 그러면 JDBC는 UTC로, 애플리케이션 코드는 KST로 동작해 **DB 왕복에서 날짜가 하루 밀린다.**
     * 실제로 밟은 버그라 둘이 일치하는지 고정한다.
     */
    @Test
    void user_timezone_프로퍼티와_JVM_기본값이_일치한다() {
        String property = System.getProperty("user.timezone");

        assertThat(property).isNotBlank();
        assertThat(ZoneId.of(property)).isEqualTo(TimeZone.getDefault().toZoneId());
    }

    @Test
    void now가_UTC가_아니라_한국_시각을_돌려준다() {
        // UTC로 찍히면 9시간 차이가 난다. 00~09시 발급분의 발급일이 전날이 되는 원인이다.
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime seoul = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toLocalDateTime();

        assertThat(Duration.between(seoul, now).abs()).isLessThan(Duration.ofSeconds(10));
    }

    @Test
    void 한국_자정_직후의_날짜가_전날로_밀리지_않는다() {
        // KST 00:30 == UTC 전날 15:30. 시간대가 UTC면 발급일이 하루 전으로 인쇄된다.
        ZonedDateTime kstJustAfterMidnight =
                ZonedDateTime.of(2026, 1, 1, 0, 30, 0, 0, ZoneId.of("Asia/Seoul"));

        LocalDateTime asDefaultZone =
                kstJustAfterMidnight.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();

        assertThat(asDefaultZone.toLocalDate()).isEqualTo(kstJustAfterMidnight.toLocalDate());
        assertThat(asDefaultZone.getYear()).isEqualTo(2026);
    }
}
