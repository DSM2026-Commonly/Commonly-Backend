package commonly.commonlybe.user;

import commonly.commonlybe.admin.entity.Admin;
import commonly.commonlybe.admin.entity.AdminRole;
import commonly.commonlybe.admin.repository.AdminRepository;
import commonly.commonlybe.petitioner.entity.Petitioner;
import commonly.commonlybe.petitioner.repository.PetitionerRepository;
import commonly.commonlybe.user.entity.User;
import commonly.commonlybe.user.repository.UserRepository;
import java.time.LocalDate;
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
class MeApiTest {

    private static final String PASSWORD = "password123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private PetitionerRepository petitionerRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (userRepository.findByAccountId("meadmin").isEmpty()) {
            User admin = userRepository.save(User.builder()
                .accountId("meadmin").password(passwordEncoder.encode(PASSWORD)).name("담당자").build());
            adminRepository.save(Admin.builder()
                .user(admin).department("민원과").role(AdminRole.ADMIN).build());
        }
        if (userRepository.findByAccountId("mecivil").isEmpty()) {
            User civil = userRepository.save(User.builder()
                .accountId("mecivil").password(passwordEncoder.encode(PASSWORD)).name("홍길동").build());
            petitionerRepository.save(Petitioner.builder()
                .user(civil).phoneNumber("010-1234-5678").birthDate(LocalDate.of(1990, 1, 1)).build());
        }
        if (userRepository.findByAccountId("meinitial").isEmpty()) {
            User initial = User.builder()
                .accountId("meinitial").password(passwordEncoder.encode(PASSWORD)).name("초기").build();
            initial.requirePasswordChange();
            userRepository.save(initial);
            adminRepository.save(Admin.builder()
                .user(initial).department("미배정").role(AdminRole.USER).build());
        }
    }

    private String tokenOf(String accountId) throws Exception {
        String body = mockMvc.perform(post("/api/auths/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"accountId": "%s", "password": "%s"}
                    """.formatted(accountId, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asString();
    }

    @Test
    void 담당자는_부서가_채워지고_민원인_필드는_비어_있다() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + tokenOf("meadmin")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value("meadmin"))
            .andExpect(jsonPath("$.name").value("담당자"))
            .andExpect(jsonPath("$.authority").value("ADMIN"))
            .andExpect(jsonPath("$.department").value("민원과"))
            .andExpect(jsonPath("$.phoneNumber").doesNotExist())
            .andExpect(jsonPath("$.birthDate").doesNotExist());
    }

    @Test
    void 민원인은_연락처와_생년월일이_채워지고_부서는_비어_있다() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + tokenOf("mecivil")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authority").value("PETITIONER"))
            .andExpect(jsonPath("$.name").value("홍길동"))
            .andExpect(jsonPath("$.phoneNumber").value("010-1234-5678"))
            .andExpect(jsonPath("$.birthDate").value("1990-01-01"))
            .andExpect(jsonPath("$.department").doesNotExist());
    }

    @Test
    void 초기_비밀번호_미변경_계정도_내_정보는_조회할_수_있다() throws Exception {
        // 이걸 막으면 FE가 비밀번호 변경 화면으로 보낼 판단 근거를 얻을 수 없다
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + tokenOf("meinitial")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.passwordChanged").value(false));
    }

    @Test
    void 초기_비밀번호_미변경_계정은_다른_API는_여전히_막힌다() throws Exception {
        // INITIAL_PASSWORD_NOT_CHANGED는 403이다
        mockMvc.perform(get("/api/admins").header("Authorization", "Bearer " + tokenOf("meinitial")))
            .andExpect(status().isForbidden());
    }

    @Test
    void 토큰이_없으면_401이다() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized());
    }
}
