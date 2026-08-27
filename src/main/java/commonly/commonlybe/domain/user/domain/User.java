package commonly.commonlybe.domain.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String accountId;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 20)
    private String name;

    // 초기 비밀번호로 생성된 계정은 비밀번호를 바꾸기 전까지 다른 API를 쓸 수 없다.
    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean passwordChanged = true;

    @Builder
    public User(String accountId, String password, String name) {
        this.accountId = accountId;
        this.password = password;
        this.name = name;
        this.passwordChanged = true;
    }

    public void requirePasswordChange() {
        this.passwordChanged = false;
    }

    public void updatePassword(String password) {
        this.password = password;
        this.passwordChanged = true;
    }

    public void updateName(String name) {
        this.name = name;
    }
}
