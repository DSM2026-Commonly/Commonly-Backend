package commonly.commonlybe.global.jwt;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class JwtGenerator {
    static final String TOKEN_TYPE_CLAIM = "typ";

    private final JwtProperties jwtProperties;

    public String generateAccessToken(String accountId) {
        return generate(accountId, TokenType.ACCESS, jwtProperties.accessExp());
    }

    public String generateRefreshToken(String accountId) {
        return generate(accountId, TokenType.REFRESH, jwtProperties.refreshExp());
    }

    /** 리프레시 토큰 만료 시각. DB 행의 expires_at을 토큰 자체의 exp와 같은 값으로 맞춘다. */
    public LocalDateTime refreshTokenExpiresAt() {
        return LocalDateTime.now(ZoneId.systemDefault()).plusSeconds(jwtProperties.refreshExp());
    }

    /**
     * jti를 넣는 이유: iat/exp가 초 단위라 같은 계정이 1초 안에 두 번 발급받으면
     * payload가 완전히 같아지고, 따라서 토큰 문자열도 똑같이 나온다.
     * refresh_token.token이 UNIQUE라 그대로 두면 두 번째 로그인이 실패한다.
     */
    private String generate(String accountId, TokenType type, int expSeconds) {
        Date now = new Date();
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(accountId)
            .claim(TOKEN_TYPE_CLAIM, type.name())
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expSeconds * 1000L))
            .signWith(jwtProperties.secretKey())
            .compact();
    }
}
