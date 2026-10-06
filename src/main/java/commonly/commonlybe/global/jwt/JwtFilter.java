package commonly.commonlybe.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    static final String REISSUE_PATH = "/api/auths/reissue";

    private final JwtProperties jwtProperties;
    private final JwtParser jwtParser;

    /**
     * 재발급은 액세스 토큰이 만료된 상태에서 부르는 API다.
     * 클라이언트가 만료된 Authorization 헤더를 그대로 달고 오면 여기서 401이 나서
     * 재발급 자체가 불가능해진다. 이 경로에서는 헤더를 아예 보지 않는다.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return REISSUE_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            Authentication authentication = jwtParser.parseToken(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String token = request.getHeader(jwtProperties.header());
        if (token != null && token.startsWith(jwtProperties.prefix())) {
            return token.substring(jwtProperties.prefix().length()).trim();
        }
        return null;
    }
}
