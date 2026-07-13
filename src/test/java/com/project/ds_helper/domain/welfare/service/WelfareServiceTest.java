package com.project.ds_helper.domain.welfare.service;

import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.welfare.dto.request.WelfareRecommendRequest;
import com.project.ds_helper.domain.welfare.dto.response.WelfareDetailResponse;
import com.project.ds_helper.domain.welfare.dto.response.WelfareListResponse;
import com.project.ds_helper.domain.welfare.dto.response.WelfareProfileResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WelfareServiceTest {

    @Mock private WelfareCommandService welfareCommandService;
    @Mock private WelfareQueryService welfareQueryService;

    @Test
    @DisplayName("복지 추천은 CommandService에 위임한다")
    void recommendWelfare_delegatesToCommandService() {
        WelfareService service = new WelfareService(welfareCommandService, welfareQueryService);
        User user = User.builder().id("u1").build();
        WelfareRecommendRequest request = WelfareRecommendRequest.builder()
                .cityProvinceName("서울특별시")
                .districtName("강남구")
                .age(25)
                .interestTheme("010")
                .build();
        when(welfareCommandService.recommendWelfare(user, request)).thenReturn(List.of(WelfareListResponse.builder().build()));

        List<WelfareListResponse> result = service.recommendWelfare(user, request);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("복지 상세 조회는 QueryService에 위임한다")
    void getWelfareDetail_delegatesToQueryService() {
        WelfareService service = new WelfareService(welfareCommandService, welfareQueryService);
        WelfareDetailResponse response = WelfareDetailResponse.builder().serviceName("복지").build();
        when(welfareQueryService.getWelfareDetail("S1")).thenReturn(response);

        assertThat(service.getWelfareDetail("S1")).isSameAs(response);
    }

    @Test
    @DisplayName("사용자 복지 프로필 조회는 QueryService에 위임한다")
    void getUserWelfareProfileResponse_delegatesToQueryService() {
        WelfareService service = new WelfareService(welfareCommandService, welfareQueryService);
        User user = User.builder().id("u1").build();
        WelfareProfileResponse response = WelfareProfileResponse.builder().id(1L).build();
        when(welfareQueryService.getUserWelfareProfileResponse(user)).thenReturn(response);

        assertThat(service.getUserWelfareProfileResponse(user)).isSameAs(response);
    }
}
