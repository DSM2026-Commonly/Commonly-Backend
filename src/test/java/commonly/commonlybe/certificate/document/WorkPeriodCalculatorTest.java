package commonly.commonlybe.certificate.document;

import static org.assertj.core.api.Assertions.assertThat;

import commonly.commonlybe.certificate.entity.CertificateEntity;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkPeriodCalculatorTest {

    @Test
    void 재직일수는_양끝을_포함한다() {
        WorkPeriod total = WorkPeriodCalculator.totalOf(
                List.of(certificate("2020-01-01", "2020-01-01", null)));

        assertThat(total).isEqualTo(new WorkPeriod(0, 1));
    }

    @Test
    void 여러_구간은_일수로_합산한_뒤_개월과_일로_되돌린다() {
        // 31일 + 30일 = 61일 -> 2개월 1일. Period끼리 더하면 "1개월 31일"이 나온다.
        WorkPeriod total = WorkPeriodCalculator.totalOf(List.of(
                certificate("2021-01-01", "2021-01-31", null),
                certificate("2021-03-01", "2021-03-30", null)));

        assertThat(total).isEqualTo(new WorkPeriod(2, 1));
    }

    @Test
    void 퇴직일이_없으면_만료예정일을_끝으로_본다() {
        WorkPeriod total = WorkPeriodCalculator.totalOf(
                List.of(certificate("2022-01-01", null, "2022-01-10")));

        assertThat(total).isEqualTo(new WorkPeriod(0, 10));
    }

    @Test
    void 끝나는_날이_없으면_그_구간은_총계에서_뺀다() {
        WorkPeriod total = WorkPeriodCalculator.totalOf(List.of(
                certificate("2022-01-01", "2022-01-10", null),
                certificate("2023-01-01", null, null)));

        assertThat(total).isEqualTo(new WorkPeriod(0, 10));
    }

    @Test
    void 채용일이_없으면_그_구간은_총계에서_뺀다() {
        WorkPeriod total = WorkPeriodCalculator.totalOf(
                List.of(certificate(null, "2022-01-10", null)));

        assertThat(total).isEqualTo(new WorkPeriod(0, 0));
    }

    @Test
    void 퇴직일이_채용일보다_앞서도_음수를_더하지_않는다() {
        WorkPeriod total = WorkPeriodCalculator.totalOf(List.of(
                certificate("2022-01-01", "2022-01-10", null),
                certificate("2023-05-01", "2023-01-01", null)));

        assertThat(total).isEqualTo(new WorkPeriod(0, 10));
    }

    @Test
    void 윤년의_2월_29일도_하루로_센다() {
        WorkPeriod total = WorkPeriodCalculator.totalOf(
                List.of(certificate("2024-02-01", "2024-02-29", null)));

        assertThat(total).isEqualTo(new WorkPeriod(0, 29));
    }

    @Test
    void 재직_이력이_없으면_0개월_0일이다() {
        assertThat(WorkPeriodCalculator.totalOf(List.of())).isEqualTo(new WorkPeriod(0, 0));
    }

    private CertificateEntity certificate(String hireDate, String retirementDate, String expirationDate) {
        return CertificateEntity.builder()
                .name("홍길동")
                .hireDate(parse(hireDate))
                .retirementDate(parse(retirementDate))
                .expirationDate(parse(expirationDate))
                .build();
    }

    private LocalDate parse(String date) {
        return date == null ? null : LocalDate.parse(date);
    }
}
