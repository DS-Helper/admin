package com.project.ds_helper.domain.reservation.enums;

import lombok.Getter;

@Getter
public enum RecipientGenderType {
    MALE("남"),
    FEMALE("여"),
    BOTH("모두");

    private final String korean;

    RecipientGenderType(String korean){this.korean = korean;}

    public static RecipientGenderType findTypeByKorean(String korean){
        for(RecipientGenderType type : values()){
            if(type.korean.equals(korean)){
                return type;
            }
        }
        throw new IllegalArgumentException("일치하는 성별 없음 : " + korean);
    }


}
