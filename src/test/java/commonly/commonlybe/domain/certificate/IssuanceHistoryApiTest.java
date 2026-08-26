package commonly.commonlybe.domain.certificate;

import commonly.commonlybe.certificate.entity.CertificateIssuedEntity;
import commonly.commonlybe.certificate.repository.CertificateIssuedRepository;
import commonly.commonlybe.domain.admin.domain.Admin;
import commonly.commonlybe.domain.admin.domain.AdminRole;
import commonly.commonlybe.domain.admin.domain.repository.AdminRepository;
import commonly.commonlybe.domain.user.domain.User;
import commonly.commonlybe.domain.user.domain.repository.UserRepository;
import commonly.commonlybe.human.entity.Gender;
import commonly.commonlybe.human.entity.HumanEntity;
import commonly.commonlybe.human.repository.HumanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IssuanceHistoryApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private HumanRepository humanRepository;
    @Autowired private CertificateIssuedRepository certificateIssuedRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        if (userRepository.findByAccountId("histadmin").isEmpty()) {
            User user = userRepository.save(User.builder()
                .accountId("histadmin").password(passwordEncoder.encode("password123"))
                .name("관리자").build());
            adminRepository.save(Admin.builder().user(user).department("미배정").role(AdminRole.ADMIN).build());

            HumanEntity hong = humanRepository.save(HumanEntity.builder()
                .name("홍길동").gender(Gender.MALE).birthDate(LocalDate.of(1990, 1, 1)).build());
            HumanEntity kim = humanRepository.save(HumanEntity.builder()
                .name("김철수").gender(Gender.MALE).birthDate(LocalDate.of(1985, 5, 5)).build());

            certificateIssuedRepository.save(CertificateIssuedEntity.builder()
                .humanId(hong.getHumanId()).documentNo("유성구-2026-000001").purpose("은행 제출")
                .totalMonths(12).totalDays(3).issuedAt(LocalDateTime.of(2026, 8, 20, 10, 0))
                .certificateIds(List.of(1L)).build());
            certificateIssuedRepository.save(CertificateIssuedEntity.builder()
                .humanId(kim.getHumanId()).documentNo("유성구-2026-000002").purpose("이직 제출")
                .totalMonths(6).totalDays(0).issuedAt(LocalDateTime.of(2026, 8, 25, 14, 0))
                .certificateIds(List.of(2L)).build());
        }

        String body = mockMvc.perform(post("/api/auths/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"accountId": "histadmin", "password": "password123"}
                    """))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        adminToken = objectMapper.readTree(body).get("accessToken").asString();
    }

    @Test
    void 발급_이력_전체_조회_최신순() throws Exception {
        mockMvc.perform(get("/api/issuance-histories")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].documentNo").value("유성구-2026-000002"))
            .andExpect(jsonPath("$[0].targetName").value("김철수"))
            .andExpect(jsonPath("$[1].documentNo").value("유성구-2026-000001"));
    }

    @Test
    void 성명_키워드_필터() throws Exception {
        mockMvc.perform(get("/api/issuance-histories")
                .header("Authorization", "Bearer " + adminToken)
                .param("keyword", "홍길"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].targetName").value("홍길동"));
    }

    @Test
    void 발급일_기간_필터() throws Exception {
        mockMvc.perform(get("/api/issuance-histories")
                .header("Authorization", "Bearer " + adminToken)
                .param("startDate", "2026-08-21")
                .param("endDate", "2026-08-26"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].documentNo").value("유성구-2026-000002"));
    }

    @Test
    void 민원인은_접근_불가() throws Exception {
        mockMvc.perform(post("/api/auths/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"accountId": "histpetit1", "password": "password123", "name": "민원인",
                     "phoneNumber": "010-1111-2222", "birthDate": "1995-01-01"}
                    """))
            .andExpect(status().isCreated());
        String body = mockMvc.perform(post("/api/auths/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"accountId": "histpetit1", "password": "password123"}
                    """))
            .andReturn().getResponse().getContentAsString();
        String petitionerToken = objectMapper.readTree(body).get("accessToken").asString();

        mockMvc.perform(get("/api/issuance-histories")
                .header("Authorization", "Bearer " + petitionerToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void size가_0이면_400() throws Exception {
        mockMvc.perform(get("/api/issuance-histories")
                .header("Authorization", "Bearer " + adminToken)
                .param("size", "0"))
            .andExpect(status().isBadRequest());
    }
}
