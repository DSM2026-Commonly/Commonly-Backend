package commonly.commonlybe.domain.fullflow;

import commonly.commonlybe.certificate.entity.CertificateEntity;
import commonly.commonlybe.certificate.repository.CertificateRepository;
import commonly.commonlybe.domain.admin.domain.Admin;
import commonly.commonlybe.domain.admin.domain.AdminRole;
import commonly.commonlybe.domain.admin.domain.repository.AdminRepository;
import commonly.commonlybe.domain.user.domain.User;
import commonly.commonlybe.domain.user.domain.repository.UserRepository;
import commonly.commonlybe.human.entity.Gender;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullFlowApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private CertificateRepository certificateRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String staffToken() throws Exception {
        if (userRepository.findByAccountId("ffstaff").isEmpty()) {
            User user = userRepository.save(User.builder()
                .accountId("ffstaff").password(passwordEncoder.encode("password123")).name("직원").build());
            adminRepository.save(Admin.builder().user(user).department("민원과").role(AdminRole.USER).build());
        }
        return login("ffstaff", "password123");
    }

    private String petitionerToken() throws Exception {
        if (userRepository.findByAccountId("ffpetit").isEmpty()) {
            mockMvc.perform(post("/api/auths/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"accountId": "ffpetit", "password": "password123", "name": "민원인",
                         "phoneNumber": "010-2222-3333", "birthDate": "1993-03-03"}
                        """))
                .andExpect(status().isCreated());
        }
        return login("ffpetit", "password123");
    }

    private String login(String accountId, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auths/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"accountId": "%s", "password": "%s"}
                    """.formatted(accountId, password)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asString();
    }

    @Test
    @Order(1)
    void 인적사항_등록_수정_검색_삭제_전체_흐름() throws Exception {
        String token = staffToken();

        String created = mockMvc.perform(post("/api/human")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "박근로", "gender": "M", "birthDate": "1988-07-07",
                     "address": "대전 유성구", "department": "시설과"}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        long humanId = objectMapper.readTree(created).get("humanId").asLong();

        mockMvc.perform(put("/api/human/" + humanId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "박근로", "gender": "M", "birthDate": "1988-07-07",
                     "address": "대전 서구", "department": "시설과"}
                    """))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/human/search")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "박근로"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].address").value("대전 서구"));

        mockMvc.perform(delete("/api/human/" + humanId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(2)
    void 경력증명서_발급_상세_수정_흐름() throws Exception {
        String token = staffToken();

        String created = mockMvc.perform(post("/api/human")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "이경력", "gender": "F", "birthDate": "1991-02-02"}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        long humanId = objectMapper.readTree(created).get("humanId").asLong();

        CertificateEntity cert = certificateRepository.save(CertificateEntity.builder()
            .humanId(humanId).name("이경력").gender(commonly.commonlybe.certificate.entity.Gender.FEMALE)
            .birthDate(LocalDate.of(1991, 2, 2))
            .hireDate(LocalDate.of(2024, 1, 1)).retirementDate(LocalDate.of(2025, 1, 1))
            .jobTitle("주무관").build());

        // 발급(POST /api/certificates)은 문서번호 채번이 Postgres 전용 네이티브 쿼리라 H2에서 검증 불가

        // humanId 기준 경력 사항 찾기
        mockMvc.perform(get("/api/humans/" + humanId + "/certificates")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        // GET /api/certificates/{id}는 발급 건 상세라 미발급 상태에서는 404
        mockMvc.perform(get("/api/certificates/" + cert.getCertificateId())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());

        // 재직 이력 수정은 certificate 행 기준
        mockMvc.perform(put("/api/certificates/" + cert.getCertificateId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "이경력", "gender": "FEMALE", "jobTitle": "팀장"}
                    """))
            .andExpect(status().isNoContent());

        // 존재하지 않는 발급 건 다운로드는 404
        mockMvc.perform(get("/api/certificates/999999/download")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    @Test
    @Order(3)
    void 민원인이_인적사항_API_호출_가능한지_확인() throws Exception {
        String token = petitionerToken();
        mockMvc.perform(post("/api/human")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "침입자", "gender": "M", "birthDate": "1990-01-01"}
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    void 민원인이_파일_업로드_API_호출_가능한지_확인() throws Exception {
        String token = petitionerToken();
        mockMvc.perform(multipart("/api/files/upload")
                .file("file", "dummy".getBytes())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }
}
