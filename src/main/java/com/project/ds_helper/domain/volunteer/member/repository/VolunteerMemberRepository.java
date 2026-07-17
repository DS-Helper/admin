package com.project.ds_helper.domain.volunteer.member.repository;

import com.project.ds_helper.domain.volunteer.common.enums.VolunteerMemberStatus;
import com.project.ds_helper.domain.volunteer.member.entity.VolunteerMember;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface VolunteerMemberRepository extends JpaRepository<VolunteerMember, String>, JpaSpecificationExecutor<VolunteerMember> {

    @EntityGraph(attributePaths = {"user", "application"})
    Optional<VolunteerMember> findByUser_Id(String userId);

    boolean existsByUser_IdAndStatusIn(String userId, Collection<VolunteerMemberStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"user", "application"})
    @Query("SELECT m FROM VolunteerMember m WHERE m.user.id = :userId")
    Optional<VolunteerMember> findByUserIdForUpdate(@Param("userId") String userId);
}
