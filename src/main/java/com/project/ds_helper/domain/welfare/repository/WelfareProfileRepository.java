package com.project.ds_helper.domain.welfare.repository;

import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.welfare.entity.WelfareProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 복지 프로필 레포지토리
 */
@Repository
public interface WelfareProfileRepository extends JpaRepository<WelfareProfile, Long> {

    /**
     * 특정 사용자의 복지 프로필을 조회합니다.
     */
    Optional<WelfareProfile> findByUser(User user);
    
    /**
     * 특정 사용자 ID로 복지 프로필을 조회합니다.
     */
    Optional<WelfareProfile> findByUserId(String userId);
}
