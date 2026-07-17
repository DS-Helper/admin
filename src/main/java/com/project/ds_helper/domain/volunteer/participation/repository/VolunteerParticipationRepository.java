package com.project.ds_helper.domain.volunteer.participation.repository;

import com.project.ds_helper.domain.volunteer.common.enums.VolunteerEventStatus;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerParticipationStatus;
import com.project.ds_helper.domain.volunteer.participation.entity.VolunteerParticipation;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VolunteerParticipationRepository extends JpaRepository<VolunteerParticipation, String> {

    @EntityGraph(attributePaths = {"event", "member", "member.application"})
    List<VolunteerParticipation> findByMember_Id(String memberId);

    @EntityGraph(attributePaths = {"member", "member.user"})
    List<VolunteerParticipation> findByEvent_Id(String eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"event", "member", "member.user"})
    @Query("""
            SELECT p FROM VolunteerParticipation p
            WHERE p.event.id = :eventId
              AND p.member.id = :memberId
            """)
    Optional<VolunteerParticipation> findByEventAndMemberForUpdate(
            @Param("eventId") String eventId,
            @Param("memberId") String memberId
    );

    @EntityGraph(attributePaths = {"event", "event.imageFile"})
    List<VolunteerParticipation> findByMember_IdAndEvent_IdIn(String memberId, Collection<String> eventIds);

    @EntityGraph(attributePaths = "event")
    List<VolunteerParticipation> findByMember_IdAndStatus(
            String memberId,
            VolunteerParticipationStatus status
    );

    long countByEvent_IdAndStatus(String eventId, VolunteerParticipationStatus status);

    @Query("""
            SELECT p.event.id AS eventId, COUNT(p.id) AS participantCount
            FROM VolunteerParticipation p
            WHERE p.event.id IN :eventIds
              AND p.status = :status
            GROUP BY p.event.id
            """)
    List<VolunteerEventParticipationCount> countByEventIdsAndStatus(
            @Param("eventIds") Collection<String> eventIds,
            @Param("status") VolunteerParticipationStatus status
    );

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
            FROM VolunteerParticipation p
            JOIN p.event e
            WHERE p.member.id = :memberId
              AND p.status = :status
              AND e.status <> :canceledEventStatus
              AND (:excludedEventId IS NULL OR e.id <> :excludedEventId)
              AND e.startAt < :newEndAt
              AND e.endAt > :newStartAt
            """)
    boolean existsTimeConflict(
            @Param("memberId") String memberId,
            @Param("status") VolunteerParticipationStatus status,
            @Param("canceledEventStatus") VolunteerEventStatus canceledEventStatus,
            @Param("excludedEventId") String excludedEventId,
            @Param("newStartAt") Instant newStartAt,
            @Param("newEndAt") Instant newEndAt
    );

    @Query("""
            SELECT p.member.user.name
            FROM VolunteerParticipation p
            WHERE p.event.id = :eventId
              AND p.status IN :statuses
            ORDER BY p.appliedAt ASC, p.id ASC
            """)
    List<String> findParticipantNames(
            @Param("eventId") String eventId,
            @Param("statuses") Collection<VolunteerParticipationStatus> statuses
    );

    @EntityGraph(attributePaths = {"event", "event.imageFile"})
    @Query(
            value = """
                    SELECT p FROM VolunteerParticipation p
                    WHERE p.member.id = :memberId
                      AND p.status = :status
                      AND p.event.startAt > :now
                      AND p.event.status <> :canceledEventStatus
                    ORDER BY p.event.startAt ASC, p.id ASC
                    """,
            countQuery = """
                    SELECT COUNT(p) FROM VolunteerParticipation p
                    WHERE p.member.id = :memberId
                      AND p.status = :status
                      AND p.event.startAt > :now
                      AND p.event.status <> :canceledEventStatus
                    """
    )
    Page<VolunteerParticipation> findUpcoming(
            @Param("memberId") String memberId,
            @Param("status") VolunteerParticipationStatus status,
            @Param("now") Instant now,
            @Param("canceledEventStatus") VolunteerEventStatus canceledEventStatus,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"event", "event.imageFile"})
    @Query(
            value = """
                    SELECT p FROM VolunteerParticipation p
                    WHERE p.member.id = :memberId
                      AND p.status = :status
                      AND p.event.status <> :canceledEventStatus
                    ORDER BY p.event.endAt DESC, p.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(p) FROM VolunteerParticipation p
                    WHERE p.member.id = :memberId
                      AND p.status = :status
                      AND p.event.status <> :canceledEventStatus
                    """
    )
    Page<VolunteerParticipation> findCompleted(
            @Param("memberId") String memberId,
            @Param("status") VolunteerParticipationStatus status,
            @Param("canceledEventStatus") VolunteerEventStatus canceledEventStatus,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "event")
    @Query("""
            SELECT p FROM VolunteerParticipation p
            WHERE p.member.id = :memberId
              AND p.status = :status
              AND p.event.status <> :canceledEventStatus
            """)
    List<VolunteerParticipation> findRecognizedParticipations(
            @Param("memberId") String memberId,
            @Param("status") VolunteerParticipationStatus status,
            @Param("canceledEventStatus") VolunteerEventStatus canceledEventStatus
    );
}
