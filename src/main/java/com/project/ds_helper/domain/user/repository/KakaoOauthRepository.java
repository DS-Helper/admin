package com.project.ds_helper.domain.user.repository;

import com.project.ds_helper.domain.user.entity.KakaoOauth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface KakaoOauthRepository extends JpaRepository<KakaoOauth, String> {

     Optional<KakaoOauth> findBySocialOauthIdAndOauthEmail(Long socialOauthId, String email);

     Optional<KakaoOauth> findByUser_Id(String userId);
}
