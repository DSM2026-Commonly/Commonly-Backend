package commonly.commonlybe.global.jwt;

import commonly.commonlybe.user.exception.UserNotFoundException;
import commonly.commonlybe.global.jwt.exception.ExpiredTokenException;
import commonly.commonlybe.global.jwt.exception.InvalidTokenException;
import commonly.commonlybe.global.security.auth.AuthDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class JwtParser {
    private final JwtProperties jwtProperties;
    private final AuthDetailsService authDetailsService;

    public Authentication parseToken(String token) {
        Claims claims = getClaims(token, TokenType.ACCESS);
        try {
            UserDetails userDetails = authDetailsService.loadUserByUsername(claims.getSubject());
            return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        } catch (UserNotFoundException e) {
            throw new InvalidTokenException();
        }
    }

    /** 리프레시 토큰의 서명·만료·타입을 검증하고 accountId를 돌려준다. */
    public String parseRefreshTokenSubject(String token) {
        return getClaims(token, TokenType.REFRESH).getSubject();
    }

    private Claims getClaims(String token, TokenType expected) {
        Claims claims;
        try {
            claims = Jwts.parser()
                .verifyWith(jwtProperties.secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException();
        } catch (Exception e) {
            throw new InvalidTokenException();
        }

        // 타입이 다르면 유효하지 않은 토큰으로 본다.
        // 어느 타입이었는지는 알려주지 않는다.
        if (!expected.name().equals(claims.get(JwtGenerator.TOKEN_TYPE_CLAIM, String.class))) {
            throw new InvalidTokenException();
        }
        return claims;
    }
}
