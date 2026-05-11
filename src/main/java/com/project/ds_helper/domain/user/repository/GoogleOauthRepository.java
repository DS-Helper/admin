package com.project.ds_helper.domain.user.repository;

import com.project.ds_helper.domain.user.entity.GoogleOauth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoogleOauthRepository extends JpaRepository<GoogleOauth, String> {

    Optional<GoogleOauth> findBySocialOauthIdAndOauthEmail(String socialOauthId, String email);

    Optional<GoogleOauth> findByUser_Id(String userId);
}
