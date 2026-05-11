package com.project.ds_helper.domain.welfare.repository;

import com.project.ds_helper.domain.welfare.entity.WelfareServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 복지 혜택 정보 레포지토리
 */
@Repository
public interface WelfareServiceRepository extends JpaRepository<WelfareServiceEntity, String> {

    /**
     * 조건에 맞는 복지 서비스를 추천 순서대로 조회합니다.
     * 1. 지역 조건(ctpvNm, sggNm)이 포함된 지자체 혜택 + 중앙부처 혜택
     * 2. 생애주기, 관심주제, 지원대상 조건 매핑 (Native Query 또는 Specification 활용 가능)
     * 여기서는 단순 조회를 위한 기본 메서드를 정의하고, 상세 로직은 Service에서 처리합니다.
     */
    @Query("SELECT w FROM WelfareServiceEntity w WHERE " +
           "(:lifeCycleCode IS NULL OR w.lifeCycleArray LIKE %:lifeCycleCode%) AND " +
           "(:interestTheme IS NULL OR w.interestThemeArray LIKE %:interestTheme%) AND " +
           "(:targetCondition IS NULL OR w.targetAudienceArray LIKE %:targetCondition%)")
    List<WelfareServiceEntity> findRecommendedWelfare(
            @Param("lifeCycleCode") String lifeCycleCode,
            @Param("interestTheme") String interestTheme,
            @Param("targetCondition") String targetCondition
    );
}
