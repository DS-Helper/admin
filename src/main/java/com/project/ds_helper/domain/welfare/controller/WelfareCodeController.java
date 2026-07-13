package com.project.ds_helper.domain.welfare.controller;

import com.project.ds_helper.common.enums.SwaggerTagName;
import com.project.ds_helper.domain.welfare.dto.response.WelfareCodeRefResponse;
import com.project.ds_helper.domain.welfare.entity.WelfareCodeGroup;
import com.project.ds_helper.domain.welfare.service.WelfareCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/welfare/codes")
@Tag(name = SwaggerTagName.WELFARE, description = "복지 코드 API")
public class WelfareCodeController {

    private final WelfareCodeService welfareCodeService;

    @Operation(summary = "복지 코드 목록 조회", description = "복지 추천/응답에 사용하는 코드 테이블을 그룹별로 조회합니다.")
    @GetMapping("/{codeGroup}")
    public ResponseEntity<List<WelfareCodeRefResponse>> getCodes(@PathVariable WelfareCodeGroup codeGroup) {
        return ResponseEntity.ok(welfareCodeService.getCodeRefs(codeGroup));
    }
}
