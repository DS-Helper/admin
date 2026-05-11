package com.project.ds_helper.common.swagger;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.ArrayList;
import java.util.List;

@OpenAPIDefinition(
        info = @Info(title = "DS-Helper Test Server API 명세서",
                version = "v1.0.0")
)
@Configuration
public class SwaggerConfig {

    @Value("${local.url}")
    private String localUrl;

    @Value("${server.url}")
    private String serverUrl;

    @Value("${test.url}")
    private String testServerUrl;

    public SwaggerConfig(MappingJackson2HttpMessageConverter converter) {
        var supportedMediaTypes = new ArrayList<>(converter.getSupportedMediaTypes());
//        supportedMediaTypes.add(new MediaType("application", "octet-stream"));
        supportedMediaTypes.add(MediaType.APPLICATION_JSON);
        supportedMediaTypes.add(MediaType.MULTIPART_FORM_DATA);
        supportedMediaTypes.add(MediaType.APPLICATION_OCTET_STREAM);
        supportedMediaTypes.add(MediaType.ALL); // .... 전체
        converter.setSupportedMediaTypes(supportedMediaTypes);

    }

    @Bean
    public OpenAPI api() {
        SecurityScheme apiKey = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("Bearer Token");

        return new OpenAPI()
                .servers(
                        List.of(
//                                new Server().url(serverUrl)
//                                new Server().url(localUrl),
                                new Server().url(testServerUrl)
                        )
                )
                .components(new Components().addSecuritySchemes("Bearer Token", apiKey))
                .addSecurityItem(securityRequirement)
        // Swagger UI에서 multipart/form-data 요청을 제대로 표시하기 위한 설정 추가
                .schema("MultipartFile", new Schema<String>().type("string").format("binary"));
    }
}