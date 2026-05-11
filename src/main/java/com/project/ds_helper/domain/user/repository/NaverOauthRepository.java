package com.project.ds_helper.domain.user.repository;

import com.project.ds_helper.domain.user.entity.NaverOauth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NaverOauthRepository extends JpaRepository<NaverOauth, String> {

    Optional<NaverOauth> findBySocialOauthIdAndOauthEmail(String socialOauthId, String email);

    Optional<NaverOauth> findByUser_Id(String userId);
}
