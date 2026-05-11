package com.project.ds_helper.domain.user.enums;

public enum OauthType {

    KAKAO("카카오"),
    GOOGLE("구글"),
    NAVER("네이버");

    private final String korean;

    OauthType(String korean){
        this.korean = korean;
    }

    public OauthType findByKorean(String korean){
        for(OauthType type : values()){
            if(type.korean.equals("korean")){
                return type;
            }
        }
        throw new IllegalArgumentException("Not Proper Type. korean : " + korean);
    }
}
