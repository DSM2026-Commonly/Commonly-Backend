package commonly.commonlybe.global.config;

import commonly.commonlybe.global.error.GlobalExceptionFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class FilterConfig implements Customizer<HttpSecurity> {
    private final ObjectMapper objectMapper;

    @Override
    public void customize(HttpSecurity http) {
        http.addFilterBefore(new GlobalExceptionFilter(objectMapper), SecurityContextHolderFilter.class);
    }
}
