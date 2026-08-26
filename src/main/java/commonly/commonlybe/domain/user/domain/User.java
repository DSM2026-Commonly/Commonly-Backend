package commonly.commonlybe.domain.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Authority authority;

    @Builder
    public User(String accountId, String password, String name, Authority authority) {
        this.accountId = accountId;
        this.password = password;
        this.name = name;
        this.authority = authority;
    }

    public void updatePassword(String password) {
        this.password = password;
    }
}
