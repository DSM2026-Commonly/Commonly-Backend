package commonly.commonlybe.certificate.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EmploymentTypeConverter implements AttributeConverter<EmploymentType, String> {

    @Override
    public String convertToDatabaseColumn(EmploymentType employmentType) {
        return employmentType == null ? null : employmentType.getLabel();
    }

    @Override
    public EmploymentType convertToEntityAttribute(String dbValue) {
        return dbValue == null ? null : EmploymentType.from(dbValue);
    }
}
