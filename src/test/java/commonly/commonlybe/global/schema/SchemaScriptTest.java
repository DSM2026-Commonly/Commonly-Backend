package commonly.commonlybe.global.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * schema.sql은 ddl-auto=validate인 운영에서 컬럼을 만드는 유일한 수단이라 매 기동 실행된다.
 * 멱등하지 않으면 두 번째 기동이 죽고, 테스트는 ddl-auto=create-drop으로 테이블을 나중에 만들기 때문에
 * 스크립트가 도는 시점에는 테이블이 아직 없다. 두 조건을 여기서 같이 지킨다 (#46).
 */
@SpringBootTest
class SchemaScriptTest {

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void schema_sql은_두_번_실행해도_깨지지_않는다() throws IOException {
        List<String> statements = statements();

        assertThat(statements).isNotEmpty();
        assertThatCode(() -> statements.forEach(jdbcTemplate::execute)).doesNotThrowAnyException();
        assertThatCode(() -> statements.forEach(jdbcTemplate::execute)).doesNotThrowAnyException();
    }

    /** 컬럼을 덧붙이는 ALTER는 테이블이 이미 있는 상태에서도 그대로 통과해야 한다. */
    @Test
    void 발급_사유_컬럼_ALTER는_컬럼이_이미_있어도_통과한다() {
        String alter = "ALTER TABLE IF EXISTS certificates_issued ADD COLUMN IF NOT EXISTS issue_reason TEXT";

        assertThatCode(() -> jdbcTemplate.execute(alter)).doesNotThrowAnyException();
        assertThatCode(() -> jdbcTemplate.execute(alter)).doesNotThrowAnyException();

        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from information_schema.columns
                where lower(table_name) = 'certificates_issued' and lower(column_name) = 'issue_reason'
                """, Integer.class)).isEqualTo(1);
    }

    private List<String> statements() throws IOException {
        String sql = new String(new ClassPathResource("schema.sql").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        return Arrays.stream(sql.split(";"))
                .map(statement -> statement.lines()
                        .filter(line -> !line.strip().startsWith("--"))
                        .reduce("", (a, b) -> a + "\n" + b)
                        .strip())
                .filter(statement -> !statement.isBlank())
                .toList();
    }
}
