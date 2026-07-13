package com.project.ds_helper.domain.welfare.service;

import com.project.ds_helper.common.enums.ErrorCode;
import com.project.ds_helper.common.exception.BusinessException;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.welfare.dto.response.WelfareDetailResponse;
import com.project.ds_helper.domain.welfare.dto.response.WelfareProfileResponse;
import com.project.ds_helper.domain.welfare.entity.WelfareCodeGroup;
import com.project.ds_helper.domain.welfare.entity.WelfareProfile;
import com.project.ds_helper.domain.welfare.entity.WelfareServiceEntity;
import com.project.ds_helper.domain.welfare.repository.WelfareProfileRepository;
import com.project.ds_helper.domain.welfare.repository.WelfareServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WelfareQueryService {

    private final WelfareServiceRepository welfareServiceRepository;
    private final WelfareProfileRepository welfareProfileRepository;
    private final WelfareSyncService welfareSyncService;
    private final WelfareCodeService welfareCodeService;

    public WelfareDetailResponse getWelfareDetail(String serviceId) {
        WelfareServiceEntity entity = welfareServiceRepository.findById(serviceId)
                .orElseGet(() -> welfareSyncService.fetchAndSaveWelfareDetailByServiceId(serviceId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Welfare service not found. serviceId=" + serviceId)));
        return WelfareDetailResponse.from(entity);
    }

    public WelfareProfileResponse getUserWelfareProfileResponse(User user) {
        WelfareProfile profile = welfareProfileRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Welfare profile not found. userId=" + user.getId()));
        return WelfareProfileResponse.builder()
                .id(profile.getId())
                .cityProvinceName(profile.getCityProvinceName())
                .districtName(profile.getDistrictName())
                .age(profile.getAge())
                .lifeCycleCode(profile.getLifeCycle())
                .lifeCycleName(welfareCodeService.resolveCodeName(WelfareCodeGroup.LIFE_ARRAY, profile.getLifeCycle()))
                .interestThemeCode(profile.getInterestTheme())
                .interestThemeName(welfareCodeService.resolveCodeName(WelfareCodeGroup.INTEREST_THEME_ARRAY, profile.getInterestTheme()))
                .targetConditionCode(profile.getTargetCondition())
                .targetConditionName(profile.getTargetCondition() == null ? null : welfareCodeService.resolveCodeName(WelfareCodeGroup.TARGET_INDV_ARRAY, profile.getTargetCondition()))
                .build();
    }
}
