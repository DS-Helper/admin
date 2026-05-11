package com.project.ds_helper.domain.server;

import com.project.ds_helper.common.enums.SwaggerTagName;

import com.project.ds_helper.common.dto.response.ResponseVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@Tag(name = SwaggerTagName.SERVER)
public class HealthCheckController {

    @GetMapping("/health-check")
    public ResponseEntity<ResponseVo> healthCheck(){
         log.debug("Health Check");
         return ResponseEntity.ok(ResponseVo.OkWithNullData());
    }
}

