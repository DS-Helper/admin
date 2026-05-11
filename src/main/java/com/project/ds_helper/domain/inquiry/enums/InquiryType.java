package com.project.ds_helper.domain.inquiry.enums;

import lombok.Getter;

@Getter
public enum InquiryType {

    HELP_REQUEST("도움 요청"),           // 도움 요청
    SERVICE_INCONVENIENCE("서비스 이용 불편"),  // 서비스 이용 불편
    SERVICE_IMPROVEMENT("서비스 개선 제안"),    // 서비스 개선 제안
    OTHER("기타");

    private final String korean;

    InquiryType(String korean){
        this.korean = korean;
    }

    public static InquiryType findByKorean(String korean){
        for(InquiryType inquiryType : values()){
            if(inquiryType.korean.equals(korean)){
                return inquiryType;
            }
        }
        throw new IllegalArgumentException("No Matched InquiryType : " + korean);
    }

}
