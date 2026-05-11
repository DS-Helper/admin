package com.project.ds_helper.domain.admin.dto.response;

import com.project.ds_helper.domain.reservation.entity.PersonalReservation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "이름으로 조회한 개인 예약 응답 DTO", example = "{\"name\":\"홍길동\",\"phoneNumber\":\"010-1234-5678\",\"visitDate\":\"2026-03-30\",\"startTime\":\"14:00\",\"endTime\":\"15:00\",\"address\":\"서울특별시 강남구 테헤란로 123\",\"requirement\":\"문서 작성 도움\",\"recipientGender\":\"남성\",\"recipientNumber\":1,\"note\":\"휠체어 접근 필요\",\"reservationStatus\":\"REQUESTED\"}")
public record GetPersonalReservationByNameResponse(
        String name, // 신청자 이름
        String phoneNumber,// 신청자 연락처
        String visitDate, // 방문 날짜
        String startTime, // 봉사 시작 시간
        String endTime, // 봉사 종료 시간
        String address, // 주소
        String requirement, // 요청 사항
        String recipientGender, // 도움 받을 사람 성별
        int recipientNumber, // 도움 받는 사람 수
        String note,
        String reservationStatus
) {
        public static GetPersonalReservationByNameResponse toDto(PersonalReservation pr){
                return new GetPersonalReservationByNameResponse(
                        pr.getName(),
                        pr.getPhoneNumber(),
                        pr.getVisitDate().toString(),
                        pr.getStartTime().toString(),
                        pr.getEndTime().toString(),
                        pr.getAddress(),
                        pr.getRequirement(),
                        pr.getRecipientGender().toString(),
                        pr.getRecipientNumber(),
                        pr.getNote(),
                        pr.getReservationStatus().toString()
                );
        }


}
