package com.project.ds_helper.domain.welfare.service;

import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.welfare.dto.request.WelfareRecommendRequest;
import com.project.ds_helper.domain.welfare.dto.response.WelfareDetailResponse;
import com.project.ds_helper.domain.welfare.dto.response.WelfareListResponse;
import com.project.ds_helper.domain.welfare.dto.response.WelfareProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WelfareService {

    private final WelfareCommandService welfareCommandService;
    private final WelfareQueryService welfareQueryService;

    public List<WelfareListResponse> recommendWelfare(User user, WelfareRecommendRequest request) {
        return welfareCommandService.recommendWelfare(user, request);
    }

    public WelfareDetailResponse getWelfareDetail(String serviceId) {
        return welfareQueryService.getWelfareDetail(serviceId);
    }

    public WelfareProfileResponse getUserWelfareProfileResponse(User user) {
        return welfareQueryService.getUserWelfareProfileResponse(user);
    }
}
