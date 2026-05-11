package com.project.ds_helper.common.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.sql.Date;
import java.time.LocalDate;

@Converter(autoApply = true)
public class LocalDateConverterConfig implements AttributeConverter<LocalDate, Date>{

        @Override
        public Date convertToDatabaseColumn(LocalDate attribute) {
            return attribute == null ? null : Date.valueOf(attribute);
        }

        @Override
        public LocalDate convertToEntityAttribute(Date dbData) {
            return dbData == null ? null : dbData.toLocalDate();
        }
}
