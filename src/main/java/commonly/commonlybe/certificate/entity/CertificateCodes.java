package commonly.commonlybe.certificate.entity;

import java.util.Set;

/**
 * 엑셀 적재(RowValidator)와 수정 API가 같은 허용값을 봐야 한다.
 * 두 벌로 두면 엑셀로는 들어가는데 API로는 막히는 상태가 된다.
 */
public final class CertificateCodes {

    public static final Set<String> VALID_DIVISIONS = Set.of("채용", "전보", "해지", "퇴직");
    public static final Set<String> VALID_EMPLOYMENT_TYPES = Set.of("기간제", "단시간근로자");

    private CertificateCodes() {
    }
}
