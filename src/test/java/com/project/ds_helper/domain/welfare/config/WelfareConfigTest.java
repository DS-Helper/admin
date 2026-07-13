package com.project.ds_helper.domain.welfare.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class WelfareConfigTest {

    @Test
    @DisplayName("복지 설정은 WebClient와 serviceKey를 제공한다")
    void createsBeans() {
        WelfareConfig config = new WelfareConfig();
        ReflectionTestUtils.setField(config, "serviceKey", "service-key");

        WebClient client = config.publicDataWebClient();
        String key = config.publicDataServiceKey();

        assertThat(client).isNotNull();
        assertThat(key).isEqualTo("service-key");
    }
}
