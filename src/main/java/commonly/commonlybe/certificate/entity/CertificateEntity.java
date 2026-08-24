package commonly.commonlybe.certificate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "gender")
    private String gender;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "key_responsibilities")
    private String keyResponsibilities;

    @Column(name = "hire_date")
    private String hireDate;

    @Column(name = "expiration_date")
    private String expirationDate;

    @Column(name = "retirement_date")
    private String retirementDate;

    @Column(name = "division")
    private String division;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "employment_type")
    private String employmentType;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Builder
    public CertificateEntity(String name, LocalDate birthDate, String gender, String jobTitle,
                              String keyResponsibilities, String hireDate, String expirationDate,
                              String retirementDate, String division, String reason,
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
        this.reason = reason;
        this.employmentType = employmentType;
        this.note = note;
    }
}
