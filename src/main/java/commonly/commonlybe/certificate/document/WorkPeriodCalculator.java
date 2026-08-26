package commonly.commonlybe.certificate.document;

import commonly.commonlybe.certificate.entity.CertificateEntity;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class WorkPeriodCalculator {

    /**
     * 경력 합산 관례: 각 구간을 일수로 환산해 더한 뒤 개월/일로 되돌린다.
     * Period끼리 더하면 정규화가 안 돼 "1개월 45일" 같은 값이 나온다.
     *
     * ponytail: 1개월 = 30일로 고정. 달마다 길이가 다른데 일수 합계에는 기준일이 없어
     * 역산할 방법이 없다. 유성구청이 다른 산정 기준(민법 기간계산 등)을 쓰면 여기만 고친다.
     */
    private static final int DAYS_PER_MONTH = 30;

    private WorkPeriodCalculator() {
    }

    public static WorkPeriod totalOf(List<CertificateEntity> certificates) {
        long totalDays = certificates.stream()
                .mapToLong(certificate -> daysOf(certificate.getHireDate(), certificate.workEndDate()))
                .sum();
        return new WorkPeriod((int) (totalDays / DAYS_PER_MONTH), (int) (totalDays % DAYS_PER_MONTH));
    }

    /**
     * 재직일수는 양끝 포함(2020-01-01 ~ 2020-01-01 = 1일).
     * 시작일이나 종료일이 없으면 그 구간은 총계에서 뺀다. 발급일까지로 임의 연장하지 않는다.
     * 종료일이 시작일보다 앞서면(데이터 오류) 음수를 더하지 않고 0으로 본다.
     */
    private static long daysOf(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(from, to) + 1;
    }
}
