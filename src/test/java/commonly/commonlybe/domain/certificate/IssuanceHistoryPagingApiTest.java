package commonly.commonlybe.domain.certificate;

import commonly.commonlybe.admin.entity.Admin;
import commonly.commonlybe.admin.entity.AdminRole;
import commonly.commonlybe.admin.repository.AdminRepository;
import commonly.commonlybe.certificate.entity.CertificateIssuedEntity;
import commonly.commonlybe.certificate.repository.CertificateIssuedRepository;
import commonly.commonlybe.human.entity.Gender;
import commonly.commonlybe.human.entity.HumanEntity;
import commonly.commonlybe.human.repository.HumanRepository;
import commonly.commonlybe.user.entity.User;
import commonly.commonlybe.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IssuanceHistoryPagingApiTest {

    private static final String PASSWORD = "password123";
    private static final String ADMIN_ACCOUNT_ID = "pagehistadmin";
    /**
     * 같은 H2 DB를 다른 테스트 클래스와 공유하므로 keyword(= 대상자 성명)로 이 테스트의 행만 좁힌다.
     * issuedAt을 과거로 둔 것도 같은 이유 - 기존 발급이력 테스트의 최신순/기간 단언을 건드리지 않는다.
     */
    private static final String TARGET_NAME = "동시발급대상자";
    private static final LocalDateTime SAME_ISSUED_AT = LocalDateTime.of(2000, 3, 1, 9, 0);
    private static final int TOTAL = 7;
    private static final int SIZE = 3;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private HumanRepository humanRepository;
    @Autowired private CertificateIssuedRepository certificateIssuedRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (userRepository.findByAccountId(ADMIN_ACCOUNT_ID).isEmpty()) {
            User admin = userRepository.save(User.builder()
                .accountId(ADMIN_ACCOUNT_ID).password(passwordEncoder.encode(PASSWORD))
                .name("관리자").build());
            adminRepository.save(Admin.builder()
                .user(admin).department("민원과").role(AdminRole.ADMIN).build());

            HumanEntity human = humanRepository.save(HumanEntity.builder()
                .name(TARGET_NAME).gender(Gender.FEMALE)
                .birthDate(LocalDate.of(1977, 7, 7)).build());

            // 발급 7건 전부 같은 issuedAt - 2차 정렬 키가 없으면 페이지마다 순서가 달라진다
            for (int i = 1; i <= TOTAL; i++) {
                certificateIssuedRepository.save(CertificateIssuedEntity.builder()
                    .humanId(human.getHumanId())
                    .documentNo("유성구-2000-%06d".formatted(i))
                    .purpose("동시 발급 검증")
                    .totalMonths(1).totalDays(0)
                    .issuedAt(SAME_ISSUED_AT)
                    .certificateIds(List.of((long) i))
                    .build());
            }
        }
    }

    private String adminToken() throws Exception {
        String body = mockMvc.perform(post("/api/auths/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"accountId": "%s", "password": "%s"}
                    """.formatted(ADMIN_ACCOUNT_ID, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asString();
    }

    private JsonNode pageOf(int page) throws Exception {
        String body = mockMvc.perform(get("/api/issuance-histories")
                .header("Authorization", "Bearer " + adminToken())
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(SIZE))
                .param("keyword", TARGET_NAME))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private List<Long> idsOf(JsonNode page) {
        List<Long> ids = new ArrayList<>();
        page.get("content").forEach(node -> ids.add(node.get("issuanceHistoryId").asLong()));
        return ids;
    }

    @Test
    void 발급시각이_같아도_페이지_간_중복이_없다() throws Exception {
        List<Long> first = idsOf(pageOf(1));
        List<Long> second = idsOf(pageOf(2));

        assertThat(first).hasSize(SIZE);
        assertThat(second).hasSize(SIZE);
        assertThat(first).doesNotContainAnyElementsOf(second);
    }

    @Test
    void 발급시각이_같아도_모든_페이지의_합집합이_전체와_같다() throws Exception {
        List<Long> collected = new ArrayList<>();
        int page = 1;
        JsonNode body;
        do {
            body = pageOf(page++);
            collected.addAll(idsOf(body));
        } while (body.get("hasNext").asBoolean());

        assertThat(body.get("totalElements").asLong()).isEqualTo(TOTAL);
        assertThat(collected).hasSize(TOTAL).doesNotHaveDuplicates();

        List<Long> expected = certificateIssuedRepository.findAll().stream()
            .filter(issued -> issued.getIssuedAt().equals(SAME_ISSUED_AT))
            .map(CertificateIssuedEntity::getCertificateIssuedId)
            .toList();
        assertThat(expected).hasSize(TOTAL);
        assertThat(collected).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void 발급시각이_같으면_식별자_내림차순으로_안정적으로_정렬된다() throws Exception {
        assertThat(idsOf(pageOf(1))).isSortedAccordingTo((a, b) -> Long.compare(b, a));
        assertThat(idsOf(pageOf(1))).isEqualTo(idsOf(pageOf(1)));
    }
}
