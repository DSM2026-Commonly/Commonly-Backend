package commonly.commonlybe.global.security;

import commonly.commonlybe.user.exception.InitialPasswordNotChangedException;
import commonly.commonlybe.global.security.auth.AuthDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 초기 비밀번호를 아직 변경하지 않은 계정은 비밀번호 변경 API만 쓸 수 있다.
 */
public class InitialPasswordFilter extends OncePerRequestFilter {
    private static final String PASSWORD_CHANGE_PATH = "/api/admin/password";
    private static final String ME_PATH = "/api/users/me";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
            && authentication.getPrincipal() instanceof AuthDetails authDetails
            && !authDetails.user().isPasswordChanged()
            && !isAllowedBeforePasswordChange(request)) {
            throw new InitialPasswordNotChangedException();
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAllowedBeforePasswordChange(HttpServletRequest request) {
        return isPasswordChangeRequest(request) || isMeRequest(request);
    }

    private boolean isPasswordChangeRequest(HttpServletRequest request) {
        return "PATCH".equals(request.getMethod()) && PASSWORD_CHANGE_PATH.equals(request.getRequestURI());
    }

    /**
     * 내 정보 조회는 막지 않는다. 클라이언트가 passwordChanged를 읽어야
     * 비밀번호 변경 화면으로 보낼 수 있는데, 이것까지 막으면 알아낼 방법이 없다.
     */
    private boolean isMeRequest(HttpServletRequest request) {
        return "GET".equals(request.getMethod()) && ME_PATH.equals(request.getRequestURI());
    }
}
