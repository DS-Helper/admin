package com.project.ds_helper.domain.welfare.service;

import com.project.ds_helper.common.exception.BusinessException;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.welfare.dto.response.WelfareDetailResponse;
import com.project.ds_helper.domain.welfare.dto.response.WelfareProfileResponse;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WelfareQueryServiceTest {

    @Mock private WelfareServiceRepository welfareServiceRepository;
    @Mock private WelfareProfileRepository welfareProfileRepository;
    @Mock private WelfareSyncService welfareSyncService;
    @Mock private WelfareCodeService welfareCodeService;

    @Test
    @DisplayName("상세가 있으면 Entity를 바로 응답으로 변환한다")
    void getWelfareDetail_returnsExistingEntity() {
        WelfareQueryService service = new WelfareQueryService(welfareServiceRepository, welfareProfileRepository, welfareSyncService, welfareCodeService);
        WelfareServiceEntity entity = WelfareServiceEntity.builder().serviceId("S1").serviceName("복지").build();
        when(welfareServiceRepository.findById("S1")).thenReturn(Optional.of(entity));

        WelfareDetailResponse response = service.getWelfareDetail("S1");

        assertThat(response.getServiceName()).isEqualTo("복지");
    }

    @Test
    @DisplayName("상세가 없으면 SyncService를 통해 다시 가져온다")
    void getWelfareDetail_fetchesWhenMissing() {
        WelfareQueryService service = new WelfareQueryService(welfareServiceRepository, welfareProfileRepository, welfareSyncService, welfareCodeService);
        WelfareServiceEntity entity = WelfareServiceEntity.builder().serviceId("S1").serviceName("복지").build();
        when(welfareServiceRepository.findById("S1")).thenReturn(Optional.empty());
        when(welfareSyncService.fetchAndSaveWelfareDetailByServiceId("S1")).thenReturn(Optional.of(entity));

        WelfareDetailResponse response = service.getWelfareDetail("S1");

        assertThat(response.getServiceName()).isEqualTo("복지");
    }

    @Test
    @DisplayName("프로필이 없으면 예외를 던진다")
    void getUserWelfareProfileResponse_throwsWhenMissing() {
        WelfareQueryService service = new WelfareQueryService(welfareServiceRepository, welfareProfileRepository, welfareSyncService, welfareCodeService);
        User user = User.builder().id("u1").build();
        when(welfareProfileRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserWelfareProfileResponse(user))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("프로필이 있으면 코드명을 함께 반환한다")
    void getUserWelfareProfileResponse_returnsCodeNames() {
        WelfareQueryService service = new WelfareQueryService(welfareServiceRepository, welfareProfileRepository, welfareSyncService, welfareCodeService);
        User user = User.builder().id("u1").build();
        WelfareProfile profile = WelfareProfile.builder()
                .id(1L)
                .user(user)
                .cityProvinceName("서울특별시")
                .districtName("강남구")
                .age(25)
                .lifeCycle("010")
                .interestTheme("020")
                .targetCondition("030")
                .build();
        when(welfareProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(welfareCodeService.resolveCodeName(WelfareCodeGroup.LIFE_ARRAY, "010")).thenReturn("영유아");
        when(welfareCodeService.resolveCodeName(WelfareCodeGroup.INTEREST_THEME_ARRAY, "020")).thenReturn("신체건강");
        when(welfareCodeService.resolveCodeName(WelfareCodeGroup.TARGET_INDV_ARRAY, "030")).thenReturn("저소득");

        WelfareProfileResponse response = service.getUserWelfareProfileResponse(user);

        assertThat(response.lifeCycleName()).isEqualTo("영유아");
        assertThat(response.interestThemeName()).isEqualTo("신체건강");
        assertThat(response.targetConditionName()).isEqualTo("저소득");
    }

    @Test
    @DisplayName("프로필의 대상 조건이 없으면 코드명 변환을 건너뛴다")
    void getUserWelfareProfileResponse_skipsTargetConditionWhenNull() {
        WelfareQueryService service = new WelfareQueryService(welfareServiceRepository, welfareProfileRepository, welfareSyncService, welfareCodeService);
        User user = User.builder().id("u1").build();
        WelfareProfile profile = WelfareProfile.builder()
                .id(1L)
                .user(user)
                .cityProvinceName("서울특별시")
                .districtName("강남구")
                .age(25)
                .lifeCycle("010")
                .interestTheme("020")
                .targetCondition(null)
                .build();
        when(welfareProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(welfareCodeService.resolveCodeName(WelfareCodeGroup.LIFE_ARRAY, "010")).thenReturn("영유아");
        when(welfareCodeService.resolveCodeName(WelfareCodeGroup.INTEREST_THEME_ARRAY, "020")).thenReturn("신체건강");

        WelfareProfileResponse response = service.getUserWelfareProfileResponse(user);

        assertThat(response.targetConditionName()).isNull();
    }
}
