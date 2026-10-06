package commonly.commonlybe.global.security.handler;

import commonly.commonlybe.global.error.error_code.ErrorProperty;
import commonly.commonlybe.global.error.response.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 시큐리티 단계(컨트롤러 진입 전)에서 끊긴 요청의 응답 본문.
 * GlobalExceptionFilter를 타지 않으므로 여기서 직접 같은 형식으로 쓴다.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, ErrorProperty errorProperty) throws IOException {
        response.setStatus(errorProperty.getStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ErrorResponse.from(errorProperty)));
    }
}
