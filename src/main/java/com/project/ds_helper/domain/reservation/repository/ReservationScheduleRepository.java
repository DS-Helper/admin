package com.project.ds_helper.domain.reservation.repository;

import com.project.ds_helper.domain.reservation.entity.ReservationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ReservationScheduleRepository extends JpaRepository<ReservationSchedule, String> {


    // ✅ 특정 날짜의 예약 목록
    List<ReservationSchedule> findByVisitDateOrderByStartTime(LocalDate visitDate);

    // ✅ 해당 날짜의 시간대가 겹치는 예약 존재 여부 확인
    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END
        FROM ReservationSchedule r
        WHERE r.visitDate = :visitDate
          AND :startTime < r.endTime
          AND :endTime > r.startTime
    """)
    boolean existsOverlap(
            @Param("visitDate") LocalDate visitDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}
