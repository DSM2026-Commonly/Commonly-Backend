package commonly.commonlybe.global.security.handler;

import commonly.commonlybe.global.error.error_code.GlobalErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 미인증 요청. 기존 HttpStatusEntryPoint는 본문 없이 401만 보내서
 * 클라이언트가 원인을 알 수 없었다.
 */
@Component
@RequiredArgsConstructor
public class CommonlyAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorWriter securityErrorWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        securityErrorWriter.write(response, GlobalErrorCode.UNAUTHORIZED);
    }
}
