package com.project.ds_helper.common.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SwaggerConfigTest {

    @Test
    @DisplayName("Swagger 설정은 서버와 보안 스키마를 포함한다")
    void api_buildsOpenApi() {
        MappingJackson2HttpMessageConverter converter = mock(MappingJackson2HttpMessageConverter.class);
        when(converter.getSupportedMediaTypes()).thenReturn(List.of(MediaType.APPLICATION_JSON));
        SwaggerConfig config = new SwaggerConfig(converter);
        ReflectionTestUtils.setField(config, "testServerUrl", "https://test.example.com");

        OpenAPI openAPI = config.api();

        assertThat(openAPI.getServers()).hasSize(1);
        assertThat(openAPI.getServers().get(0).getUrl()).isEqualTo("https://test.example.com");
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("Bearer Token");
    }
}
