package com.project.ds_helper.domain.welfare.controller;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.welfare.dto.request.WelfareRecommendRequest;
import com.project.ds_helper.domain.welfare.dto.response.WelfareDetailResponse;
import com.project.ds_helper.domain.welfare.dto.response.WelfareListResponse;
import com.project.ds_helper.domain.welfare.service.WelfareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 복지 서비스 추천 및 조회 컨트롤러
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/welfare")
@Tag(name = "Welfare", description = "복지 서비스 추천 및 조회 API")
public class WelfareController {

    private final WelfareService welfareService;
    private final UserUtil userUtil;

    /**
     * 사용자의 정보를 입력받아 맞춤 복지 혜택을 추천합니다.
     * 
     * @param authentication 인증 정보 (JWT)
     * @param request 추천 요청 데이터
     * @return 추천된 복지 혜택 목록
     */
    @Operation(summary = "맞춤 복지 혜택 추천", description = "사용자 입력을 기반으로 복지 혜택을 추천하고 프로필을 저장합니다.")
    @PostMapping("/recommend")
    public ResponseEntity<List<WelfareListResponse>> recommendWelfare(
            Authentication authentication,
            @Valid @RequestBody WelfareRecommendRequest request
    ) {
        // 1. 인증 정보에서 사용자 식별자 추출
        String userId = userUtil.extractUserId(authentication);
        
        // 2. 사용자 엔티티 조회
        User user = userUtil.findUserById(userId);
        
        // 3. 추천 로직 실행 및 결과 반환
        List<WelfareListResponse> response = welfareService.recommendWelfare(user, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 복지 혜택의 상세 정보를 조회합니다.
     * 
     * @param serviceId 서비스 식별자
     * @return 복지 혜택 상세 정보
     */
    @Operation(summary = "복지 혜택 상세 조회", description = "특정 복지 서비스의 상세 내용을 조회합니다.")
    @GetMapping("/detail/{serviceId}")
    public ResponseEntity<WelfareDetailResponse> getWelfareDetail(@PathVariable String serviceId) {
        WelfareDetailResponse response = welfareService.getWelfareDetail(serviceId);
        return ResponseEntity.ok(response);
    }
}
