package com.project.ds_helper.common.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WebConfigTest {

    @Test
    @DisplayName("CORS 설정에 허용 오리진과 메서드가 포함된다")
    void addCorsMappings_registersCors() {
        WebConfig webConfig = new WebConfig();
        RecordingCorsRegistry registry = new RecordingCorsRegistry();

        webConfig.addCorsMappings(registry);

        assertThat(registry.registration.allowedOrigins).contains("http://localhost:3000", "https://server.dshelper.kr");
        assertThat(registry.registration.allowedMethods).contains("GET", "POST", "DELETE");
        assertThat(registry.registration.allowCredentials).isTrue();
    }

    private static final class RecordingCorsRegistry extends CorsRegistry {
        private final RecordingCorsRegistration registration = new RecordingCorsRegistration("/**");

        @Override
        public CorsRegistration addMapping(String pathPattern) {
            return registration;
        }
    }

    private static final class RecordingCorsRegistration extends CorsRegistration {
        private List<String> allowedOrigins;
        private List<String> allowedMethods;
        private Boolean allowCredentials;

        private RecordingCorsRegistration(String pathPattern) {
            super(pathPattern);
        }

        @Override
        public CorsRegistration allowedOrigins(String... origins) {
            this.allowedOrigins = List.of(origins);
            return this;
        }

        @Override
        public CorsRegistration allowedMethods(String... methods) {
            this.allowedMethods = List.of(methods);
            return this;
        }

        @Override
        public CorsRegistration allowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
            return this;
        }
    }
}
