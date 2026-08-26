package commonly.commonlybe.global.config;

import commonly.commonlybe.global.error.GlobalExceptionFilter;
import commonly.commonlybe.global.jwt.JwtFilter;
import commonly.commonlybe.global.jwt.JwtParser;
import commonly.commonlybe.global.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class FilterConfig extends AbstractHttpConfigurer<FilterConfig, HttpSecurity> {
    private final ObjectMapper objectMapper;
    private final JwtProperties jwtProperties;
    private final JwtParser jwtParser;

    @Override
    public void configure(HttpSecurity http) {
        http.addFilterBefore(new JwtFilter(jwtProperties, jwtParser), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(new GlobalExceptionFilter(objectMapper), JwtFilter.class);
    }
}
