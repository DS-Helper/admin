package com.project.ds_helper.domain.welfare.service;

import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.welfare.dto.request.WelfareRecommendRequest;
import com.project.ds_helper.domain.welfare.dto.response.WelfareCodeRefResponse;
import com.project.ds_helper.domain.welfare.dto.response.WelfareListResponse;
import com.project.ds_helper.domain.welfare.entity.WelfareCodeGroup;
import com.project.ds_helper.domain.welfare.entity.WelfareProfile;
import com.project.ds_helper.domain.welfare.entity.WelfareServiceEntity;
import com.project.ds_helper.domain.welfare.repository.WelfareProfileRepository;
import com.project.ds_helper.domain.welfare.repository.WelfareServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class WelfareCommandService {

    private final WelfareServiceRepository welfareServiceRepository;
    private final WelfareProfileRepository welfareProfileRepository;
    private final WelfareCodeService welfareCodeService;

    public List<WelfareListResponse> recommendWelfare(User user, WelfareRecommendRequest request) {
        String lifeCycleCode = welfareCodeService.resolveLifeCycleCode(request.getAge());
        validateCode(WelfareCodeGroup.INTEREST_THEME_ARRAY, request.getInterestTheme());
        if (request.getTargetCondition() != null && !"선택안함".equals(request.getTargetCondition())) {
            validateCode(WelfareCodeGroup.TARGET_INDV_ARRAY, request.getTargetCondition());
        }

        WelfareProfile profile = welfareProfileRepository.findByUser(user)
                .orElse(WelfareProfile.builder().user(user).build());
        profile.updateProfile(
                request.getCityProvinceName(),
                request.getDistrictName(),
                request.getAge(),
                lifeCycleCode,
                request.getInterestTheme(),
                request.getTargetCondition()
        );
        welfareProfileRepository.save(profile);

        String targetCondition = normalizeOptionalCode(request.getTargetCondition());
        List<WelfareServiceEntity> candidateEntities = welfareServiceRepository.findCandidateWelfare(request.getCityProvinceName(), request.getDistrictName());
        return candidateEntities.stream()
                .filter(entity -> matchesCodes(entity.getLifeCycleArray(), lifeCycleCode))
                .filter(entity -> matchesCodes(entity.getInterestThemeArray(), request.getInterestTheme()))
                .filter(entity -> targetCondition == null || matchesCodes(entity.getTargetAudienceArray(), targetCondition))
                .map(this::toWelfareListResponse)
                .collect(Collectors.toList());
    }

    private void validateCode(WelfareCodeGroup codeGroup, String codeValue) {
        if (!welfareCodeService.existsActiveCode(codeGroup, codeValue)) {
            throw new IllegalArgumentException("Invalid welfare code. group=" + codeGroup + ", code=" + codeValue);
        }
    }

    private WelfareListResponse toWelfareListResponse(WelfareServiceEntity entity) {
        return WelfareListResponse.from(entity);
    }

    private boolean matchesCodes(String rawCodes, String requiredCode) {
        if (rawCodes == null || rawCodes.isBlank() || requiredCode == null || requiredCode.isBlank()) {
            return false;
        }
        Set<String> codes = Arrays.stream(rawCodes.split("[,\\s]+"))
                .filter(code -> !code.isBlank())
                .collect(Collectors.toCollection(HashSet::new));
        return codes.contains(requiredCode);
    }

    private String normalizeOptionalCode(String codeValue) {
        return codeValue == null || codeValue.isBlank() || "선택안함".equals(codeValue) ? null : codeValue;
    }
}
