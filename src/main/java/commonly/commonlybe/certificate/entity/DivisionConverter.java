package commonly.commonlybe.certificate.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DivisionConverter implements AttributeConverter<Division, String> {

    @Override
    public String convertToDatabaseColumn(Division division) {
        return division == null ? null : division.getLabel();
    }

    @Override
    public Division convertToEntityAttribute(String dbValue) {
        return dbValue == null ? null : Division.from(dbValue);
    }
}
