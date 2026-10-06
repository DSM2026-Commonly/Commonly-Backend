package commonly.commonlybe.global.security;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationStatusTest {

    private static final String ACCOUNT_ID = "forbiddenuser";
    private static final String PASSWORD = "password123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Admin 레코드를 만들지 않으므로 권한은 PETITIONER가 된다
        if (userRepository.findByAccountId(ACCOUNT_ID).isEmpty()) {
            userRepository.save(User.builder()
                .accountId(ACCOUNT_ID)
                .password(passwordEncoder.encode(PASSWORD))
                .name("민원인")
                .build());
        }
    }

    private String petitionerToken() throws Exception {
        String body = mockMvc.perform(post("/api/auths/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"accountId": "%s", "password": "%s"}
                    """.formatted(ACCOUNT_ID, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asString();
    }

    @Test
    void 권한이_부족하면_403과_본문을_준다() throws Exception {
        mockMvc.perform(get("/api/admins").header("Authorization", "Bearer " + petitionerToken()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").exists());
        // code 필드는 #47에서 추가된다
    }

    @Test
    void 토큰이_없으면_401과_본문을_준다() throws Exception {
        mockMvc.perform(get("/api/admins"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void 인증_실패와_권한_부족이_서로_다른_상태코드다() throws Exception {
        // FE가 세션 만료와 권한 부족을 구분할 수 있어야 한다
        mockMvc.perform(get("/api/admins"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admins").header("Authorization", "Bearer " + petitionerToken()))
            .andExpect(status().isForbidden());
    }
}
