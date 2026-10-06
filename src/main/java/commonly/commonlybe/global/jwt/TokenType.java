package commonly.commonlybe.global.jwt;

/**
 * 액세스 토큰과 리프레시 토큰은 같은 키로 서명된다.
 * 타입 구분이 없으면 리프레시 토큰을 그대로 Authorization 헤더에 넣어 API를 호출할 수 있다.
 * 모든 토큰에 이 값을 claim으로 박고, 쓰는 쪽에서 기대하는 타입인지 확인한다.
 */
public enum TokenType {
    ACCESS,
    REFRESH
}
