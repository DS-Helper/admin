package com.project.ds_helper.domain.welfare.util;

import lombok.experimental.UtilityClass;

/**
 * 복지 서비스 관련 유틸리티 클래스
 */
@UtilityClass
public class WelfareUtils {

    /**
     * 사용자의 나이를 기반으로 공공데이터 생애주기(lifeArray) 코드로 변환합니다.
     * 
     * @param age 사용자 나이
     * @return 생애주기 코드 (예: "001", "002" 등)
     */
    public static String convertAgeToLifeCycleCode(int age) {
        // 0 ~ 5세: 영유아 (001)
        if (age >= 0 && age <= 5) {
            return "001";
        }
        // 6 ~ 12세: 아동 (002)
        else if (age >= 6 && age <= 12) {
            return "002";
        }
        // 13 ~ 18세: 청소년 (003)
        else if (age >= 13 && age <= 18) {
            return "003";
        }
        // 19 ~ 39세: 청년 (004)
        else if (age >= 19 && age <= 39) {
            return "004";
        }
        // 40 ~ 64세: 중장년 (005)
        else if (age >= 40 && age <= 64) {
            return "005";
        }
        // 65세 이상: 노년 (006)
        else {
            return "006";
        }
    }

    /**
     * 생애주기 코드에 대한 한글 명칭을 반환합니다. (디버깅/참고용)
     */
    public static String getLifeCycleName(String code) {
        return switch (code) {
            case "001" -> "영유아";
            case "002" -> "아동";
            case "003" -> "청소년";
            case "004" -> "청년";
            case "005" -> "중장년";
            case "006" -> "노년";
            case "007" -> "임신/출산";
            default -> "알 수 없음";
        };
    }
}
