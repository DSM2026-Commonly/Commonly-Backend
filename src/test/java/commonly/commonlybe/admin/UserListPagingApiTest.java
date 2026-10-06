package commonly.commonlybe.admin;

import commonly.commonlybe.admin.entity.Admin;
import commonly.commonlybe.admin.entity.AdminRole;
import commonly.commonlybe.admin.repository.AdminRepository;
import commonly.commonlybe.user.entity.User;
import commonly.commonlybe.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
class UserListPagingApiTest {

    private static final String PASSWORD = "password123";
    private static final String ADMIN_ACCOUNT_ID = "pageadmin";
    /** 다른 테스트 클래스가 만든 USER 계정과 섞이지 않도록 keyword로 이 테스트의 데이터만 좁힌다. */
    private static final String KEYWORD = "페이징대상";
    private static final int TOTAL = 7;
    private static final int SIZE = 3;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (userRepository.findByAccountId(ADMIN_ACCOUNT_ID).isEmpty()) {
            User admin = userRepository.save(User.builder()
                .accountId(ADMIN_ACCOUNT_ID).password(passwordEncoder.encode(PASSWORD))
                .name("관리자").build());
            adminRepository.save(Admin.builder()
                .user(admin).department("민원과").role(AdminRole.ADMIN).build());

            for (int i = 1; i <= TOTAL; i++) {
                User user = userRepository.save(User.builder()
                    .accountId("pageuser%02d".formatted(i))
                    .password(passwordEncoder.encode(PASSWORD))
                    .name(KEYWORD + "%02d".formatted(i))
                    .build());
                adminRepository.save(Admin.builder()
                    .user(user).department("미배정").role(AdminRole.USER).build());
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
        String body = mockMvc.perform(get("/api/admins")
                .header("Authorization", "Bearer " + adminToken())
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(SIZE))
                .param("keyword", KEYWORD))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private List<Long> userIdsOf(JsonNode page) {
        List<Long> ids = new ArrayList<>();
        page.get("content").forEach(node -> ids.add(node.get("userId").asLong()));
        return ids;
    }

    @Test
    void 사용자_목록은_페이지를_넘겨도_중복이_없다() throws Exception {
        List<Long> first = userIdsOf(pageOf(1));
        List<Long> second = userIdsOf(pageOf(2));

        assertThat(first).hasSize(SIZE);
        assertThat(second).hasSize(SIZE);
        assertThat(first).doesNotContainAnyElementsOf(second);
    }

    @Test
    void 모든_페이지를_합치면_전체_사용자가_정확히_한_번씩_나온다() throws Exception {
        List<Long> collected = new ArrayList<>();
        int page = 1;
        JsonNode body;
        do {
            body = pageOf(page++);
            collected.addAll(userIdsOf(body));
        } while (body.get("hasNext").asBoolean());

        assertThat(body.get("totalElements").asLong()).isEqualTo(TOTAL);
        assertThat(collected).hasSize(TOTAL).doesNotHaveDuplicates();

        Set<Long> expected = new HashSet<>();
        for (int i = 1; i <= TOTAL; i++) {
            expected.add(userRepository.findByAccountId("pageuser%02d".formatted(i)).orElseThrow().getId());
        }
        assertThat(collected).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void 같은_페이지를_두_번_불러도_같은_순서를_준다() throws Exception {
        // 정렬 프로퍼티가 잘못되면 500으로, 정렬이 빠지면 순서 흔들림으로 여기서 드러난다
        assertThat(userIdsOf(pageOf(1))).isEqualTo(userIdsOf(pageOf(1)));
        assertThat(userIdsOf(pageOf(1))).isSorted();
    }
}
