package com.project.ds_helper.domain.welfare.service;

import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.welfare.dto.request.WelfareRecommendRequest;
import com.project.ds_helper.domain.welfare.dto.response.WelfareDetailResponse;
import com.project.ds_helper.domain.welfare.dto.response.WelfareListResponse;
import com.project.ds_helper.domain.welfare.entity.WelfareProfile;
import com.project.ds_helper.domain.welfare.entity.WelfareServiceEntity;
import com.project.ds_helper.domain.welfare.repository.WelfareProfileRepository;
import com.project.ds_helper.domain.welfare.repository.WelfareServiceRepository;
import com.project.ds_helper.domain.welfare.util.WelfareUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 복지 서비스 비즈니스 로직 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WelfareService {

    private final WelfareServiceRepository welfareServiceRepository;
    private final WelfareProfileRepository welfareProfileRepository;

    /**
     * 사용자의 정보를 기반으로 맞춤 복지 혜택을 추천합니다.
     * 
     * @param user 현재 로그인한 사용자
     * @param request 추천 요청 데이터 (지역, 나이 등)
     * @return 추천된 복지 혜택 목록
     */
    @Transactional
    public List<WelfareListResponse> recommendWelfare(User user, WelfareRecommendRequest request) {
        log.info("사용자 {}의 복지 추천 시작: 지역={}, 나이={}", user.getId(), request.getDistrictName(), request.getAge());

        // 1. 나이를 기반으로 생애주기 코드로 변환 (동작 규칙 4-2 반영)
        String lifeCycleCode = WelfareUtils.convertAgeToLifeCycleCode(request.getAge());
        
        // 2. 사용자 복지 프로필 저장 또는 갱신 (동작 규칙 7 반영)
        WelfareProfile profile = welfareProfileRepository.findByUser(user)
                .orElse(WelfareProfile.builder().user(user).build());
        
        profile.updateProfile(
                request.getCityProvinceName(),
                request.getDistrictName(),
                request.getAge(),
                lifeCycleCode,
                request.getInterestTheme(),
                request.getTargetCondition()
        );
        welfareProfileRepository.save(profile);

        // 3. DB에서 추천 기준에 맞는 혜택 조회 (추천 기준 1~4 반영)
        // 지역 필터링은 현재 단순화를 위해 생략하거나 Repository 쿼리에서 처리하도록 설계
        // 실제 운영 시에는 '선택안함' 처리 로직(동작 규칙 4-4)이 포함됩니다.
        String targetCondition = "선택안함".equals(request.getTargetCondition()) ? null : request.getTargetCondition();
        
        List<WelfareServiceEntity> recommendedEntities = welfareServiceRepository.findRecommendedWelfare(
                lifeCycleCode,
                request.getInterestTheme(),
                targetCondition
        );

        // 4. 결과를 DTO로 변환하여 반환
        return recommendedEntities.stream()
                .map(WelfareListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 복지 혜택의 상세 정보를 조회합니다.
     * 
     * @param serviceId 서비스 식별자
     * @return 상세 정보 응답 DTO
     */
    public WelfareDetailResponse getWelfareDetail(String serviceId) {
        log.info("복지 혜택 상세 조회 시작: serviceId={}", serviceId);

        WelfareServiceEntity entity = welfareServiceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 복지 서비스입니다. serviceId=" + serviceId));

        return WelfareDetailResponse.from(entity);
    }

    /**
     * 저장된 사용자 복지 프로필을 조회합니다.
     */
    public WelfareProfile getUserWelfareProfile(User user) {
        return welfareProfileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("사용자 복지 프로필이 존재하지 않습니다."));
    }
}
