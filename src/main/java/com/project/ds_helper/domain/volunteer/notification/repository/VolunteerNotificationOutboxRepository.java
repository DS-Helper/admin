package com.project.ds_helper.domain.volunteer.notification.repository;

import com.project.ds_helper.domain.volunteer.notification.entity.VolunteerNotificationOutbox;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerOutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface VolunteerNotificationOutboxRepository extends JpaRepository<VolunteerNotificationOutbox, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o FROM VolunteerNotificationOutbox o
            WHERE o.status IN :statuses
              AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= :now)
            ORDER BY o.createdAt ASC, o.id ASC
            """)
    List<VolunteerNotificationOutbox> findReadyForDispatch(
            @Param("statuses") Collection<VolunteerOutboxStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable
    );
}
