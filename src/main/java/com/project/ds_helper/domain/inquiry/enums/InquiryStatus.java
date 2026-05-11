package com.project.ds_helper.domain.inquiry.enums;

import lombok.Getter;

@Getter
public enum InquiryStatus {

    UNANSWERED("답변 대기"),
    ANSWERED("답변 보기"); // 취소 상태 유무 확인 필요 or DB 삭제 처리

    private final String korean;

    InquiryStatus(String korean){
        this.korean = korean;
    }
}
