package commonly.commonlybe.domain.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CorsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 프리플라이트_요청이_모든_Origin에_허용된다() throws Exception {
        mockMvc.perform(options("/api/auths/login")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
            .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test
    void HEAD_프리플라이트도_허용된다() throws Exception {
        mockMvc.perform(options("/api/admins")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "HEAD")
                .header("Access-Control-Request-Headers", "Authorization"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void 인증_필요한_경로도_프리플라이트는_통과한다() throws Exception {
        mockMvc.perform(options("/api/admins")
                .header("Origin", "https://commonly.example.com")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "https://commonly.example.com"));
    }

    /**
     * allowedHeaders("*")는 요청 헤더 허용이라 응답 노출과 무관하다.
     * Expose-Headers가 빠지면 FE가 다운로드 파일명을 읽지 못한다.
     */
    @Test
    void 프리플라이트_응답이_Content_Disposition을_노출_대상으로_알려준다() throws Exception {
        mockMvc.perform(options("/api/certificates/1/download")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Expose-Headers", "Content-Disposition"));
    }

    @Test
    void 실제_요청_응답에도_Content_Disposition이_노출된다() throws Exception {
        mockMvc.perform(get("/api/certificates/1/download")
                .header("Origin", "http://localhost:3000"))
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
            .andExpect(header().string("Access-Control-Expose-Headers", "Content-Disposition"));
    }
}
