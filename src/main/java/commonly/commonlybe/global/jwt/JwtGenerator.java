package commonly.commonlybe.global.jwt;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

@RequiredArgsConstructor
@Component
public class JwtGenerator {
    private final JwtProperties jwtProperties;

    public String generateAccessToken(String accountId) {
        Date now = new Date();
        return Jwts.builder()
            .subject(accountId)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + jwtProperties.accessExp() * 1000L))
            .signWith(jwtProperties.secretKey())
            .compact();
    }
}
