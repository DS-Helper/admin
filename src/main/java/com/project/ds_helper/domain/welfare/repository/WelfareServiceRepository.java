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

    @Query("SELECT w FROM WelfareServiceEntity w WHERE " +
           "w.active = true AND " +
           "(:cityProvinceName IS NULL OR w.cityProvinceName = :cityProvinceName) AND " +
           "(:districtName IS NULL OR w.districtName = :districtName)")
    List<WelfareServiceEntity> findCandidateWelfare(
            @Param("cityProvinceName") String cityProvinceName,
            @Param("districtName") String districtName
    );

    List<WelfareServiceEntity> findAllByActiveTrue();

    List<WelfareServiceEntity> findAllByActiveTrueAndDetailSyncFailedTrue();
}
