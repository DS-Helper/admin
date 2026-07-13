package com.project.ds_helper.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectMapperUtilTest {

    @Test
    @DisplayName("customObjectMapper는 JavaTimeModule과 timestamp 비활성화를 가진다")
    void customObjectMapper_configuresModules() {
        ObjectMapper objectMapper = new ObjectMapperUtil().customObjectMapper();

        assertThat(objectMapper.getRegisteredModuleIds()).isNotEmpty();
        assertThat(objectMapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
    }
}
