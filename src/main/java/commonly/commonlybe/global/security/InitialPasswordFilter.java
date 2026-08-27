package commonly.commonlybe.global.security;

import commonly.commonlybe.domain.user.exception.InitialPasswordNotChangedException;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
            && authentication.getPrincipal() instanceof AuthDetails authDetails
            && !authDetails.user().isPasswordChanged()
            && !isPasswordChangeRequest(request)) {
            throw new InitialPasswordNotChangedException();
        }
        filterChain.doFilter(request, response);
    }

    private boolean isPasswordChangeRequest(HttpServletRequest request) {
        return "PATCH".equals(request.getMethod()) && PASSWORD_CHANGE_PATH.equals(request.getRequestURI());
    }
}
