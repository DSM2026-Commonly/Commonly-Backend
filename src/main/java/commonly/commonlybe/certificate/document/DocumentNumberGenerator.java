package commonly.commonlybe.certificate.document;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DocumentNumberGenerator {

    /**
     * 한 문장이라 원자적이다. 연도 행이 없으면 1부터 시작한다.
     * Postgres 시퀀스를 안 쓴 이유: 연도별로 000001부터 다시 시작해야 하는데 시퀀스는 연초 리셋이 필요하다.
     *
     * ponytail: 연도 행 하나에 걸리는 행 잠금이라 동시 발급이 직렬화된다.
     * 구청 창구 발급량에선 문제없음. 초당 수백 건이 되면 연도+구간 분할로 올린다.
     */
    private static final String NEXT_NUMBER_SQL = """
            insert into document_number_seq (year, last_no) values (:year, 1)
            on conflict (year) do update set last_no = document_number_seq.last_no + 1
            returning last_no
            """;

    private final EntityManager entityManager;

    /** 발급 트랜잭션 안에서만 부른다. 밖에서 부르면 발급이 실패해도 번호가 빠진다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public String generate(int year) {
        Number lastNo = (Number) entityManager.createNativeQuery(NEXT_NUMBER_SQL)
                .setParameter("year", year)
                .getSingleResult();
        return "유성구-%d-%06d".formatted(year, lastNo.longValue());
    }
}
