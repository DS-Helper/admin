package com.project.ds_helper.domain.reservation.enums;

import lombok.Getter;

@Getter
public enum ReservationStatus {
     REQUESTED("대기"),
//    ACCEPTED("수락"),
//    STARTED("시작"),
    COMPLETED("완료"),
    CANCELED("취소");

    private final String korean;

     ReservationStatus(String koraen){
         this.korean = koraen;
     }

    // 한국어로 일치하는 status 찾기
     public static ReservationStatus findStatusByString(String korean){
        for(ReservationStatus status : values()){
            if(status.getKorean().equals(korean)){
                return status;
            }
        }
        throw new IllegalArgumentException("No ReservationStatus Matched" + korean);
     }
}
