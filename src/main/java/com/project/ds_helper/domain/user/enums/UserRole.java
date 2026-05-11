package com.project.ds_helper.domain.user.enums;

public enum UserRole {

    USER("유저"),
    HELPER("헬퍼"),
    ADMIN("관리자");
    
    private final String korean;

    UserRole(String korean){
        this.korean = korean;
    }

    public UserRole findByKorean(String korean){
        for(UserRole role : values()){
            if(role.korean.equals("korean")){
                return role;
            }
        }
        throw new IllegalArgumentException("Not Proper Role. korean : " + korean);
    }
}
