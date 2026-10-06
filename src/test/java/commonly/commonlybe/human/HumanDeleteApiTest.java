package commonly.commonlybe.human;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import commonly.commonlybe.admin.entity.Admin;
import commonly.commonlybe.admin.entity.AdminRole;
import commonly.commonlybe.admin.repository.AdminRepository;
import commonly.commonlybe.certificate.entity.CertificateEntity;
import commonly.commonlybe.certificate.entity.CertificateIssuedEntity;
import commonly.commonlybe.certificate.repository.CertificateIssuedRepository;
import commonly.commonlybe.certificate.repository.CertificateRepository;
import commonly.commonlybe.human.entity.Gender;
import commonly.commonlybe.human.entity.HumanEntity;
import commonly.commonlybe.human.repository.HumanRepository;
import commonly.commonlybe.user.entity.User;
import commonly.commonlybe.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * 인적사항 삭제 가드(#64). 트랜잭션 롤백을 쓰지 않는다 —
 * 409가 난 뒤 DB에 실제로 무엇이 남았는지가 확인 대상이기 때문이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HumanDeleteApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private HumanRepository humanRepository;
    @Autowired private CertificateRepository certificateRepository;
    @Autowired private CertificateIssuedRepository certificateIssuedRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String staffToken;

    @BeforeEach
    void setUp() throws Exception {
        if (userRepository.findByAccountId("delstaff").isEmpty()) {
            User user = userRepository.save(User.builder()
                    .accountId("delstaff").password(passwordEncoder.encode("password123"))
                    .name("삭제담당").build());
            adminRepository.save(Admin.builder()
                    .user(user).department("민원과").role(AdminRole.USER).build());
        }

        String body = mockMvc.perform(post("/api/auths/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId": "delstaff", "password": "password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        staffToken = objectMapper.readTree(body).get("accessToken").asString();
    }

    @Test
    void 연결된_데이터가_없으면_204로_삭제된다() throws Exception {
        HumanEntity human = humanRepository.save(human("삭제가능자", LocalDate.of(1991, 1, 11)));

        mockMvc.perform(delete("/api/human/" + human.getHumanId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isNoContent());

        assertThat(humanRepository.existsById(human.getHumanId())).isFalse();
    }

    @Test
    void 재직_이력이_있으면_409와_HUMAN_HAS_CERTIFICATE를_준다() throws Exception {
        HumanEntity human = humanRepository.save(human("재직이력보유자", LocalDate.of(1992, 2, 22)));
        certificateRepository.save(CertificateEntity.builder()
                .humanId(human.getHumanId()).name(human.getName())
                .birthDate(human.getBirthDate())
                .gender(commonly.commonlybe.certificate.entity.Gender.MALE)
                .division("채용").hireDate(LocalDate.of(2024, 3, 1))
                .build());

        mockMvc.perform(delete("/api/human/" + human.getHumanId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("HUMAN_HAS_CERTIFICATE"));

        assertThat(humanRepository.existsById(human.getHumanId())).isTrue();
    }

    /**
     * 발급 건이 조용히 사라지던 경로가 이 테스트의 본체다.
     * 409만 확인하면 부족하다 — 인적사항과 발급 건이 둘 다 그대로 남아야 한다.
     */
    @Test
    void 발급_건이_있으면_409를_주고_인적사항과_발급_건이_그대로_남는다() throws Exception {
        HumanEntity human = humanRepository.save(human("발급이력보유자", LocalDate.of(1993, 3, 23)));
        CertificateIssuedEntity issued = certificateIssuedRepository.save(CertificateIssuedEntity.builder()
                .humanId(human.getHumanId()).documentNo("유성구-2026-064001")
                .purpose("은행 제출").totalMonths(12).totalDays(0)
                .issuedAt(LocalDateTime.of(2026, 9, 1, 9, 0))
                .certificateIds(List.of(1L))
                .build());

        mockMvc.perform(delete("/api/human/" + human.getHumanId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("HUMAN_HAS_ISSUED_CERTIFICATE"));

        assertThat(humanRepository.existsById(human.getHumanId())).isTrue();
        assertThat(certificateIssuedRepository.existsById(issued.getCertificateIssuedId())).isTrue();
    }

    private HumanEntity human(String name, LocalDate birthDate) {
        return HumanEntity.builder()
                .name(name).gender(Gender.MALE).birthDate(birthDate)
                .address("대전 유성구").department("시설과")
                .build();
    }
}
