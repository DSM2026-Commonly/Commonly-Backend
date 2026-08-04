package commonly.commonlybe.certificate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
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

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "birth_date")
    private LocalDateTime birthDate;

    @Column(name = "gender", length = 1, nullable = false)
    private String gender;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "key_responsibilities")
    private String keyResponsibilities;

    @Column(name = "hire_date")
    private LocalDateTime hireDate;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    @Column(name = "retirement_date")
    private LocalDateTime retirementDate;

    @Column(name = "division")
    private Division division;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "employment_type")
    private EmploymentType employmentType;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Builder
    public CertificateEntity(String name, LocalDateTime birthDate, String gender, String jobTitle,
                              String keyResponsibilities, LocalDateTime hireDate, LocalDateTime expirationDate,
                              LocalDateTime retirementDate, Division division, String reason,
                              EmploymentType employmentType, String note) {
        this.name = name;
        this.birthDate = birthDate;
        this.gender = gender;
        this.jobTitle = jobTitle;
        this.keyResponsibilities = keyResponsibilities;
        this.hireDate = hireDate;
        this.expirationDate = expirationDate;
        this.retirementDate = retirementDate;
        this.division = division;
        this.reason = reason;
        this.employmentType = employmentType;
        this.note = note;
    }
}
