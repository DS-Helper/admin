package com.project.ds_helper.domain.welfare.service;

import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.welfare.dto.request.WelfareRecommendRequest;
import com.project.ds_helper.domain.welfare.entity.WelfareCodeGroup;
import com.project.ds_helper.domain.welfare.entity.WelfareProfile;
import com.project.ds_helper.domain.welfare.entity.WelfareServiceEntity;
import com.project.ds_helper.domain.welfare.repository.WelfareProfileRepository;
import com.project.ds_helper.domain.welfare.repository.WelfareServiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WelfareCommandServiceTest {

    @Mock private WelfareServiceRepository welfareServiceRepository;
    @Mock private WelfareProfileRepository welfareProfileRepository;
    @Mock private WelfareCodeService welfareCodeService;

    @Test
    @DisplayName("추천은 코드 검증 후 프로필 저장과 결과 반환을 수행한다")
    void recommendWelfare_returnsMatchedWelfare() {
        WelfareCommandService service = new WelfareCommandService(welfareServiceRepository, welfareProfileRepository, welfareCodeService);
        User user = User.builder().id("u1").build();
        WelfareRecommendRequest request = WelfareRecommendRequest.builder()
                .cityProvinceName("서울특별시")
                .districtName("강남구")
                .age(25)
                .interestTheme("010")
                .targetCondition("020")
                .build();
        when(welfareCodeService.resolveLifeCycleCode(25)).thenReturn("004");
        when(welfareCodeService.existsActiveCode(WelfareCodeGroup.INTEREST_THEME_ARRAY, "010")).thenReturn(true);
        when(welfareCodeService.existsActiveCode(WelfareCodeGroup.TARGET_INDV_ARRAY, "020")).thenReturn(true);
        when(welfareProfileRepository.findByUser(user)).thenReturn(Optional.empty());
        when(welfareServiceRepository.findCandidateWelfare("서울특별시", "강남구")).thenReturn(List.of(
                WelfareServiceEntity.builder()
                        .serviceId("S1")
                        .lifeCycleArray("004")
                        .interestThemeArray("010")
                        .targetAudienceArray("020")
                        .serviceName("복지")
                        .build()
        ));

        var result = service.recommendWelfare(user, request);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("관심주제 코드가 없으면 예외를 던진다")
    void recommendWelfare_throwsWhenInterestCodeMissing() {
        WelfareCommandService service = new WelfareCommandService(welfareServiceRepository, welfareProfileRepository, welfareCodeService);
        User user = User.builder().id("u1").build();
        WelfareRecommendRequest request = WelfareRecommendRequest.builder()
                .cityProvinceName("서울특별시")
                .districtName("강남구")
                .age(25)
                .interestTheme("999")
                .build();
        when(welfareCodeService.resolveLifeCycleCode(25)).thenReturn("004");
        when(welfareCodeService.existsActiveCode(WelfareCodeGroup.INTEREST_THEME_ARRAY, "999")).thenReturn(false);

        assertThatThrownBy(() -> service.recommendWelfare(user, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("선택안함은 대상 조건 검증을 생략한다")
    void recommendWelfare_skipsOptionalTargetCondition() {
        WelfareCommandService service = new WelfareCommandService(welfareServiceRepository, welfareProfileRepository, welfareCodeService);
        User user = User.builder().id("u1").build();
        WelfareRecommendRequest request = WelfareRecommendRequest.builder()
                .cityProvinceName("서울특별시")
                .districtName("강남구")
                .age(25)
                .interestTheme("010")
                .targetCondition("선택안함")
                .build();
        when(welfareCodeService.resolveLifeCycleCode(25)).thenReturn("004");
        when(welfareCodeService.existsActiveCode(WelfareCodeGroup.INTEREST_THEME_ARRAY, "010")).thenReturn(true);
        when(welfareProfileRepository.findByUser(user)).thenReturn(Optional.empty());
        when(welfareServiceRepository.findCandidateWelfare("서울특별시", "강남구")).thenReturn(List.of());

        var result = service.recommendWelfare(user, request);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("추천 필터는 대상 조건이 없으면 대상 배열을 확인하지 않는다")
    void recommendWelfare_skipsTargetFilterWhenConditionNull() {
        WelfareCommandService service = new WelfareCommandService(welfareServiceRepository, welfareProfileRepository, welfareCodeService);
        User user = User.builder().id("u1").build();
        WelfareRecommendRequest request = WelfareRecommendRequest.builder()
                .cityProvinceName("서울특별시")
                .districtName("강남구")
                .age(25)
                .interestTheme("010")
                .build();
        when(welfareCodeService.resolveLifeCycleCode(25)).thenReturn("004");
        when(welfareCodeService.existsActiveCode(WelfareCodeGroup.INTEREST_THEME_ARRAY, "010")).thenReturn(true);
        when(welfareProfileRepository.findByUser(user)).thenReturn(Optional.empty());
        when(welfareServiceRepository.findCandidateWelfare("서울특별시", "강남구")).thenReturn(List.of(
                WelfareServiceEntity.builder()
                        .serviceId("S1")
                        .lifeCycleArray("004")
                        .interestThemeArray("010")
                        .serviceName("복지")
                        .build()
        ));

        var result = service.recommendWelfare(user, request);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("대상 조건 정규화는 공백과 선택안함을 null로 바꾼다")
    void normalizeOptionalCode_mapsBlankAndSelectedToNull() {
        WelfareCommandService service = new WelfareCommandService(welfareServiceRepository, welfareProfileRepository, welfareCodeService);

        Object nullValue = ReflectionTestUtils.invokeMethod(service, "normalizeOptionalCode", (String) null);
        Object blankValue = ReflectionTestUtils.invokeMethod(service, "normalizeOptionalCode", " ");
        Object selectedValue = ReflectionTestUtils.invokeMethod(service, "normalizeOptionalCode", "선택안함");

        assertThat(nullValue).isNull();
        assertThat(blankValue).isNull();
        assertThat(selectedValue).isNull();
    }
}
