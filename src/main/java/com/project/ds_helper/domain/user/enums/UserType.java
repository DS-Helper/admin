package com.project.ds_helper.domain.user.enums;

public enum UserType {
    
    PERSONAL("개인"),
    ORGANIZATION("기관");

    private final String korean;

    UserType(String korean){
        this.korean = korean;
    }

    public UserType findByKorean(String korean){
        for(UserType type : values()){
            if(type.korean.equals("korean")){
                return type;
            }
        }
        throw new IllegalArgumentException("Not Proper Type. korean : " + korean);
    }
}
