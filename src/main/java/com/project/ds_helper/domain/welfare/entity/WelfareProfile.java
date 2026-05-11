package com.project.ds_helper.domain.welfare.entity;

import com.project.ds_helper.domain.base.entity.BaseTime;
import com.project.ds_helper.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자 복지 프로필 엔티티
 * 사용자가 입력한 추천 조건(지역, 나이, 도움 유형 등)을 저장하여 재추천 및 관리에 활용합니다.
 */
@Entity
@Table(name = "tb_welfare_profile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Builder
public class WelfareProfile extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "welfare_profile_id")
    private Long id;

    // 1:1 관계 - 사용자 한 명당 하나의 복지 프로필을 가짐
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "city_province_name", nullable = false)
    private String cityProvinceName; // 거주 지역 (시도명, 예: 경상남도)

    @Column(name = "district_name", nullable = false)
    private String districtName; // 거주 지역 (시군구명, 예: 창원시)

    @Column(name = "age", nullable = false)
    private Integer age; // 사용자 실제 나이

    @Column(name = "life_cycle", nullable = false)
    private String lifeCycle; // 내부 변환된 생애주기 코드 (예: 001, 002)

    @Column(name = "interest_theme", nullable = false)
    private String interestTheme; // 필요한 도움 유형 (관심주제 분류 코드)

    @Column(name = "target_condition")
    private String targetCondition; // 해당 조건 (지원대상 분류 코드, 선택사항)

    /**
     * 프로필 정보를 업데이트하는 메서드
     */
    public void updateProfile(String cityProvinceName, String districtName, Integer age, String lifeCycle, String interestTheme, String targetCondition) {
        this.cityProvinceName = cityProvinceName;
        this.districtName = districtName;
        this.age = age;
        this.lifeCycle = lifeCycle;
        this.interestTheme = interestTheme;
        this.targetCondition = targetCondition;
    }
}
