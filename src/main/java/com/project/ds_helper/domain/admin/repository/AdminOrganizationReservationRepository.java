package com.project.ds_helper.domain.admin.repository;

import com.project.ds_helper.domain.reservation.entity.OrganizationReservation;
import com.project.ds_helper.domain.reservation.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminOrganizationReservationRepository extends JpaRepository<OrganizationReservation, String>, JpaSpecificationExecutor<OrganizationReservation> {
    @EntityGraph(attributePaths = {"reservationSchedule"})
    Page<OrganizationReservation> findAllByReservationStatus(ReservationStatus reservationStatus, Pageable pageRequest);

    long countByReservationStatus(ReservationStatus reservationStatus);
}
