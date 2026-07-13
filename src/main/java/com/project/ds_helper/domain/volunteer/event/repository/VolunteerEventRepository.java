package com.project.ds_helper.domain.volunteer.event.repository;

import com.project.ds_helper.domain.volunteer.common.enums.VolunteerEventCloseReason;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerEventStatus;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerEventVisibility;
import com.project.ds_helper.domain.volunteer.event.entity.VolunteerEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface VolunteerEventRepository extends JpaRepository<VolunteerEvent, String> {

    @EntityGraph(attributePaths = "imageFile")
    @Query(
            value = """
                    SELECT e FROM VolunteerEvent e
                    WHERE e.visibility = :visibility
                      AND e.status IN :visibleStatuses
                      AND (:status IS NULL OR e.status = :status)
                    ORDER BY e.startAt ASC, e.id ASC
                    """,
            countQuery = """
                    SELECT COUNT(e) FROM VolunteerEvent e
                    WHERE e.visibility = :visibility
                      AND e.status IN :visibleStatuses
                      AND (:status IS NULL OR e.status = :status)
                    """
    )
    Page<VolunteerEvent> findVisibleEvents(
            @Param("visibility") VolunteerEventVisibility visibility,
            @Param("visibleStatuses") java.util.Collection<VolunteerEventStatus> visibleStatuses,
            @Param("status") VolunteerEventStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "imageFile")
    @Query("""
            SELECT e FROM VolunteerEvent e
            WHERE e.id = :eventId
              AND e.visibility = :visibility
              AND e.status <> :draftStatus
            """)
    Optional<VolunteerEvent> findUserVisibleById(
            @Param("eventId") String eventId,
            @Param("visibility") VolunteerEventVisibility visibility,
            @Param("draftStatus") VolunteerEventStatus draftStatus
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "imageFile")
    @Query("SELECT e FROM VolunteerEvent e WHERE e.id = :eventId")
    Optional<VolunteerEvent> findByIdForUpdate(@Param("eventId") String eventId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE VolunteerEvent e
               SET e.status = :closedStatus,
                   e.closeReason = :deadlineReason
             WHERE e.status = :openStatus
               AND e.recruitmentDeadlineAt <= :now
            """)
    int closeExpiredRecruitments(
            @Param("now") Instant now,
            @Param("openStatus") VolunteerEventStatus openStatus,
            @Param("closedStatus") VolunteerEventStatus closedStatus,
            @Param("deadlineReason") VolunteerEventCloseReason deadlineReason
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE VolunteerEvent e
               SET e.status = :completedStatus
             WHERE e.status IN :completableStatuses
               AND e.endAt <= :now
            """)
    int completeFinishedEvents(
            @Param("now") Instant now,
            @Param("completableStatuses") java.util.Collection<VolunteerEventStatus> completableStatuses,
            @Param("completedStatus") VolunteerEventStatus completedStatus
    );
}
