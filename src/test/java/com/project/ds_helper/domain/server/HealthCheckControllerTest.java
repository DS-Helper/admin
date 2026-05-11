package com.project.ds_helper.domain.server;

import com.project.ds_helper.common.dto.response.ResponseVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class HealthCheckControllerTest {

    private final HealthCheckController healthCheckController = new HealthCheckController();

    @Test
    @DisplayName("헬스체크는 200 OK와 공통 응답을 반환한다")
    void healthCheck_returnsOkResponse() {
        ResponseEntity<ResponseVo> response = healthCheckController.healthCheck();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isNull();
    }
}
