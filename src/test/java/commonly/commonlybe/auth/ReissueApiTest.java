package commonly.commonlybe.auth;

import commonly.commonlybe.auth.repository.RefreshTokenRepository;
import commonly.commonlybe.global.jwt.JwtGenerator;
import commonly.commonlybe.user.entity.User;
import commonly.commonlybe.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReissueApiTest {

    private static final String ACCOUNT_ID = "reissueuser";
    private static final String PASSWORD = "password123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private JwtGenerator jwtGenerator;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (userRepository.findByAccountId(ACCOUNT_ID).isEmpty()) {
            userRepository.save(User.builder()
                .accountId(ACCOUNT_ID)
                .password(passwordEncoder.encode(PASSWORD))
                .name("재발급")
                .build());
        }
    }

    private String login() throws Exception {
        String body = mockMvc.perform(post("/api/auths/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"accountId": "%s", "password": "%s"}
                    """.formatted(ACCOUNT_ID, PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("refreshToken").asString();
    }

    private String reissue(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auths/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken": "%s"}
                    """.formatted(refreshToken)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    }

    @Test
    void 리프레시_토큰으로_액세스_토큰을_재발급한다() throws Exception {
        String refreshToken = login();

        String body = reissue(refreshToken);

        assertThat(objectMapper.readTree(body).get("accessToken").asString()).isNotBlank();
        assertThat(objectMapper.readTree(body).get("refreshToken").asString()).isNotBlank();
    }

    @Test
    void 재발급하면_리프레시_토큰이_회전되어_이전_토큰은_무효가_된다() throws Exception {
        String oldToken = login();

        String newToken = objectMapper.readTree(reissue(oldToken)).get("refreshToken").asString();
        assertThat(newToken).isNotEqualTo(oldToken);

        // 이미 쓴 토큰은 두 번 통하지 않는다
        mockMvc.perform(post("/api/auths/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken": "%s"}
                    """.formatted(oldToken)))
            .andExpect(status().isUnauthorized());

        assertThat(refreshTokenRepository.findByToken(oldToken)).isEmpty();
        assertThat(refreshTokenRepository.findByToken(newToken)).isPresent();
    }

    @Test
    void 재발급받은_액세스_토큰으로_보호된_API를_호출할_수_있다() throws Exception {
        String body = reissue(login());
        String accessToken = objectMapper.readTree(body).get("accessToken").asString();

        // 민원인 권한이라 403이 기대값이다. 401(인증 실패)이 아니면 토큰은 정상 동작한 것이다.
        mockMvc.perform(get("/api/admins").header("Authorization", "Bearer " + accessToken))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    void 액세스_토큰으로는_재발급할_수_없다() throws Exception {
        String accessToken = jwtGenerator.generateAccessToken(ACCOUNT_ID);

        mockMvc.perform(post("/api/auths/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken": "%s"}
                    """.formatted(accessToken)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void 리프레시_토큰으로는_보호된_API를_호출할_수_없다() throws Exception {
        String refreshToken = login();

        mockMvc.perform(get("/api/admins").header("Authorization", "Bearer " + refreshToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void 만료된_액세스_토큰을_헤더에_달고_와도_재발급된다() throws Exception {
        String refreshToken = login();

        mockMvc.perform(post("/api/auths/reissue")
                .header("Authorization", "Bearer this.is.not.a.valid.token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken": "%s"}
                    """.formatted(refreshToken)))
            .andExpect(status().isOk());
    }

    @Test
    void 위조된_리프레시_토큰은_401이다() throws Exception {
        mockMvc.perform(post("/api/auths/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJoYWNrZXIifQ.bogus"}
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void 리프레시_토큰이_비어_있으면_400이다() throws Exception {
        mockMvc.perform(post("/api/auths/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken": ""}
                    """))
            .andExpect(status().isBadRequest());
    }
}
