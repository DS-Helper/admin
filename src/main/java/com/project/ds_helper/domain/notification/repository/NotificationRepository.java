package com.project.ds_helper.domain.notification.repository;

import com.project.ds_helper.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    @Query("""
        SELECT n FROM Notification n
        WHERE n.user.id = :userId
        AND (
            :cursorTime IS NULL
            OR n.createdAt < :cursorTime
            OR (n.createdAt = :cursorTime AND n.id < :cursorId)
        )
        ORDER BY n.createdAt DESC, n.id DESC
    """)
    List<Notification> findNotificationsWithCursor(
            @Param("userId") String userId,
            @Param("cursorTime") LocalDateTime cursorTime,
            @Param("cursorId") String cursorId,
            Pageable pageable
    );

    Optional<Notification> findByIdAndUser_Id(String notificationId, String userId);

    long countByUser_IdAndIsReadFalse(String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Notification n
        SET n.isRead = true
        WHERE n.user.id = :userId
        AND n.isRead = false
    """)
    int markAllAsReadByUserId(@Param("userId") String userId);
}
