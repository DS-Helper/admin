package com.project.ds_helper.domain.reservation.entity;

import com.project.ds_helper.common.config.LocalDateConverterConfig;
import com.project.ds_helper.domain.base.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "tb_reservation_schedule")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ReservationSchedule extends BaseTime {

    @PrePersist
    public void generateId(){
        this.id = UUID.randomUUID().toString();
    }

    @Id
    @Column(name = "reservation_schedule_id")
    private String id;

    @Column(name = "visit_date")
    @Convert(converter = LocalDateConverterConfig.class)
    private LocalDate visitDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;


}
