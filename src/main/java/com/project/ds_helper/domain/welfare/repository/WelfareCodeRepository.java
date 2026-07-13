package com.project.ds_helper.domain.welfare.repository;

import com.project.ds_helper.domain.welfare.entity.WelfareCodeEntity;
import com.project.ds_helper.domain.welfare.entity.WelfareCodeGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WelfareCodeRepository extends JpaRepository<WelfareCodeEntity, String> {

    List<WelfareCodeEntity> findAllByCodeGroupAndActiveTrueOrderBySortOrderAsc(WelfareCodeGroup codeGroup);

    List<WelfareCodeEntity> findAllByCodeGroupOrderBySortOrderAsc(WelfareCodeGroup codeGroup);

    Optional<WelfareCodeEntity> findByCodeGroupAndCodeValueAndActiveTrue(WelfareCodeGroup codeGroup, String codeValue);
}
