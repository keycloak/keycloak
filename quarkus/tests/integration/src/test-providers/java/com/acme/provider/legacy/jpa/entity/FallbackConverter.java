package com.acme.provider.legacy.jpa.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class FallbackConverter implements AttributeConverter<Boolean, String> {

    @Override
    public String convertToDatabaseColumn(Boolean attribute) {
        return String.valueOf(attribute);
    }

    @Override
    public Boolean convertToEntityAttribute(String dbData) {
        return Boolean.valueOf(dbData);
    }
}
