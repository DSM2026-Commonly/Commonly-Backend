package commonly.commonlybe.domain.admin.domain;

import commonly.commonlybe.domain.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Admin {
    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 30)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AdminRole role;

    @Builder
    public Admin(User user, String department, AdminRole role) {
        this.user = user;
        this.department = department;
        this.role = role;
    }
}
