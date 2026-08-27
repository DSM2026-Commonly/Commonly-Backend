package commonly.commonlybe.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final FilterConfig filterConfig;

    /**
     * 본인 발급 스위치. 기본값 false, 운영에서는 끈 채로 둔다.
     * 회원가입에 신원 검증이 없어 (성명, 생년월일)만 알면 남의 인적사항에 붙을 수 있다.
     * PetitionerHumanResolver 주석 참고.
     */
    @Value("${app.certificate.self-issue-enabled:false}")
    private boolean selfIssueEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .authorizeHttpRequests(auth -> {
                auth
                    .requestMatchers("/api/auths/login", "/api/auths/signup").permitAll()
                    .requestMatchers("/api/admin/password").hasAnyAuthority("ADMIN", "USER")
                    .requestMatchers("/api/admin/**", "/api/admins").hasAuthority("ADMIN");

                // 본인 발급. 담당자는 /api/certificates를 쓴다.
                // 신원 검증이 붙기 전까지는 기본 차단이고, 명시적으로 켠 환경에서만 열린다.
                var selfIssue = auth.requestMatchers(HttpMethod.POST, "/api/certificates/self");
                if (selfIssueEnabled) {
                    selfIssue.hasAuthority("PETITIONER");
                } else {
                    selfIssue.denyAll();
                }

                auth
                    // 다운로드는 담당자와 발급 대상자 모두. 소유권 검사는 CertificateService에서.
                    .requestMatchers(HttpMethod.GET, "/api/certificates/*/download").authenticated()
                    // 나머지 경력증명서 API는 전부 민원 담당자용이다.
                    .requestMatchers("/api/certificates/**", "/api/humans/*/certificates")
                        .hasAnyAuthority("ADMIN", "USER")
                    .anyRequest().authenticated();
            })
            .with(filterConfig, Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
