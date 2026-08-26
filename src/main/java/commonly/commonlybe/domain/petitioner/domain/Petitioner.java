package commonly.commonlybe.domain.petitioner.domain;

import commonly.commonlybe.domain.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Petitioner {
    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 13)
    private String phoneNumber;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Builder
    public Petitioner(User user, String phoneNumber, LocalDate birthDate) {
        this.user = user;
        this.phoneNumber = phoneNumber;
        this.birthDate = birthDate;
    }
}
