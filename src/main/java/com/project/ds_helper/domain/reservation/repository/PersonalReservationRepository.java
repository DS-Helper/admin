package com.project.ds_helper.domain.reservation.repository;

import com.project.ds_helper.domain.reservation.dto.response.GetPreReservedReservationByDateResDto;
import com.project.ds_helper.domain.reservation.entity.OrganizationReservation;
import com.project.ds_helper.domain.reservation.entity.PersonalReservation;
import com.project.ds_helper.domain.reservation.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PersonalReservationRepository extends JpaRepository<PersonalReservation, String> {

    @EntityGraph(attributePaths = {"user"})
    List<PersonalReservation> findAllByUser_Id(String userId);

    @EntityGraph(attributePaths = {"user"})
    Page<PersonalReservation> findAllByUser_IdAndReservationStatus(String userId, ReservationStatus reservationStatus, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Slice<PersonalReservation> findAllByUser_IdAndReservationStatusOrderByCreatedAtDesc(String userId, ReservationStatus reservationStatus, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Slice<PersonalReservation> findAllByUser_IdOrderByCreatedAtDesc(String userId, Pageable pageable);

//    @Query(value = "SELECT new com.project.ds_helper.domain.reservation.dto.response.GetPreReservedReservationByDateResDto(p.startTime, p.endTime) FROM PersonalReservation p")
//    List<GetPreReservedReservationByDateResDto> findAllByReservationScheduleVisitDateOrderByReservationScheduleVisitTimeAsc(LocalDate date);


    boolean existsByUser_IdAndReservationStatus(String userId, ReservationStatus reservationStatus); // 여러 예약 상태에대해 조회하는 확장성을 고려한다면 In, List.of()를 고려

    List<PersonalReservation> findAllByNameOrderByCreatedAtDesc(String name);
}
