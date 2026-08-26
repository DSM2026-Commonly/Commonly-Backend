package commonly.commonlybe.certificate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "certificate")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CertificateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "certificate_id")
    private Long certificateId;

    /** 인적사항(humans) 연결. 엑셀 적재분은 humans를 거치지 않으므로 nullable이다. */
    @Column(name = "human_id")
    private Long humanId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "key_responsibilities")
    private String keyResponsibilities;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "retirement_date")
    private LocalDate retirementDate;

    @Column(name = "division")
    private String division;

    @Column(name = "department")
    private String department;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "employment_type")
    private String employmentType;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Builder
    public CertificateEntity(Long humanId, String name, LocalDate birthDate, Gender gender, String jobTitle,
                              String keyResponsibilities, LocalDate hireDate, LocalDate expirationDate,
                              LocalDate retirementDate, String division, String department, String reason,
                              String employmentType, String note) {
        this.humanId = humanId;
        this.name = name;
        this.birthDate = birthDate;
        this.gender = gender;
        this.jobTitle = jobTitle;
        this.keyResponsibilities = keyResponsibilities;
        this.hireDate = hireDate;
        this.expirationDate = expirationDate;
        this.retirementDate = retirementDate;
        this.division = division;
        this.department = department;
        this.reason = reason;
        this.employmentType = employmentType;
        this.note = note;
    }

    public void update(String name, LocalDate birthDate, Gender gender, String jobTitle,
                       String keyResponsibilities, LocalDate hireDate, LocalDate expirationDate,
                       LocalDate retirementDate, String division, String department, String reason,
                       String employmentType, String note) {
        this.name = name;
        this.birthDate = birthDate;
        this.gender = gender;
        this.jobTitle = jobTitle;
        this.keyResponsibilities = keyResponsibilities;
        this.hireDate = hireDate;
        this.expirationDate = expirationDate;
        this.retirementDate = retirementDate;
        this.division = division;
        this.department = department;
        this.reason = reason;
        this.employmentType = employmentType;
        this.note = note;
    }

    /** 서식 근무기간의 "까지". 퇴직일이 없으면 만료예정일, 둘 다 없으면 null(재직 중). */
    public LocalDate workEndDate() {
        return retirementDate != null ? retirementDate : expirationDate;
    }
}
