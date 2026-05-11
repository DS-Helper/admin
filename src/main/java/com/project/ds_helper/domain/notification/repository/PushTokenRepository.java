package com.project.ds_helper.domain.notification.repository;

import com.project.ds_helper.domain.notification.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PushTokenRepository extends JpaRepository<PushToken, String> {

    Optional<PushToken> findByUser_IdAndToken(String userId, String token);

    Optional<PushToken> findByIdAndUser_Id(String pushTokenId, String userId);

    List<PushToken> findAllByUser_IdAndIsActiveTrue(String userId);

    List<PushToken> findAllByUser_IdInAndIsActiveTrue(Collection<String> userIds);
}
