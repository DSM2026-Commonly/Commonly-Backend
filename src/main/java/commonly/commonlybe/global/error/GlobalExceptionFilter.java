package commonly.commonlybe.global.error;

import commonly.commonlybe.global.error.error_code.GlobalErrorCode;
import commonly.commonlybe.global.error.exception.CommonlyException;
import commonly.commonlybe.global.error.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (CommonlyException e) {
            writeResponse(response, toErrorResponse(e), e.getErrorProperty().getStatus().value());
        } catch (Exception e) {
            if (e.getCause() instanceof CommonlyException commonlyException) {
                writeResponse(response, toErrorResponse(commonlyException), commonlyException.getErrorProperty().getStatus().value());
            } else {
                log.error("예상하지 못한 에러 : ", e);
                writeResponse(response, toErrorResponse(e), HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        }
    }

    private ErrorResponse toErrorResponse(Exception e) {
        if (e instanceof CommonlyException commonlyException)
            return e.getMessage() != null ? ErrorResponse.from(commonlyException.getErrorProperty()) : ErrorResponse.of(commonlyException.getErrorProperty(), commonlyException.getMessage());
        else
            return ErrorResponse.from(GlobalErrorCode.INTERNAL_SERVER_ERROR);
    }

    private void writeResponse(HttpServletResponse response, ErrorResponse errorResponse, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
