package commonly.commonlybe.global.security.handler;

import commonly.commonlybe.global.error.error_code.GlobalErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 인증은 됐고 권한만 부족한 요청. 401이 아니라 403이다.
 *
 * 핸들러가 없으면 AccessDeniedException이 authenticationEntryPoint로 흘러 401이 된다.
 * 클라이언트가 "로그인 안 됨"으로 오해해 토큰 재발급을 시도하면 무한 반복이 된다.
 */
@Component
@RequiredArgsConstructor
public class CommonlyAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorWriter securityErrorWriter;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        securityErrorWriter.write(response, GlobalErrorCode.ACCESS_DENIED);
    }
}
