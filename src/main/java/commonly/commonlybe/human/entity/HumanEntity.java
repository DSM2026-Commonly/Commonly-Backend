package commonly.commonlybe.human.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "humans",
        uniqueConstraints = @UniqueConstraint(name = "uk_humans_name_birth_date",
                columnNames = {"name", "birth_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HumanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "human_id")
    private Long humanId;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "address")
    private String address;

    @Column(name = "department")
    private String department;

    @Builder
    public HumanEntity(String name, Gender gender, LocalDate birthDate, String address, String department) {
        this.name = name;
        this.gender = gender;
        this.birthDate = birthDate;
        this.address = address;
        this.department = department;
    }

    public void update(String name, Gender gender, LocalDate birthDate, String address, String department) {
        this.name = name;
        this.gender = gender;
        this.birthDate = birthDate;
        this.address = address;
        this.department = department;
    }
}
