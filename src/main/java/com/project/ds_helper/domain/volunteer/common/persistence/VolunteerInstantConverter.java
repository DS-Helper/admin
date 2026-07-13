package com.project.ds_helper.domain.volunteer.common.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Converter
public class VolunteerInstantConverter implements AttributeConverter<Instant, LocalDateTime> {

    @Override
    public LocalDateTime convertToDatabaseColumn(Instant attribute) {
        return attribute == null ? null : LocalDateTime.ofInstant(attribute, ZoneOffset.UTC);
    }

    @Override
    public Instant convertToEntityAttribute(LocalDateTime dbData) {
        return dbData == null ? null : dbData.toInstant(ZoneOffset.UTC);
    }
}
