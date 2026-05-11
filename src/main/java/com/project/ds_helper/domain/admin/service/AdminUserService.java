package com.project.ds_helper.domain.admin.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.admin.dto.response.GetPersonalReservationByNameResponse;
import com.project.ds_helper.domain.admin.repository.AdminUserRepository;
import com.project.ds_helper.domain.reservation.entity.PersonalReservation;
import com.project.ds_helper.domain.reservation.repository.PersonalReservationRepository;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final PersonalReservationRepository personalReservationRepository;
    private final UserUtil userUtil;

    /**
     * 가입된 유저 수 조회
     **/
    public Object userCount(Authentication authentication) {

        String role = "USER";
        int userCount = adminUserRepository.countByRole(role);
        HashMap<String, Integer> responseDto = new HashMap<>();
        responseDto.put("userCount", userCount);
        return responseDto;
    }

    /**
     * name 기반 유저 조회
     * 동일 이름 유저가 여러명일 경우 모두 반환
     * **/
    public Object getUserInfoByName(String name, Authentication authentication) {
        
        // 유저 조회 및 검증
        String userId = userUtil.extractUserId(authentication);
        log.debug("AdminUserService.getUserInfoByName started. requesterUserId={}", userId);

        User user = userUtil.findUserById(userId);
        UserRole userRole = user.getRole();
        log.debug("AdminUserService.getUserInfoByName requester role resolved. userRole={}", userRole.name());

        // 관리자만 접근 가능
        if(!userRole.equals(UserRole.ADMIN)){
            throw new AccessDeniedException("Only Admin Can Access");
        }
        
        // 예약 내역 조회, dto 변환 후 반환
        List<PersonalReservation> personalReservations = personalReservationRepository.findAllByNameOrderByCreatedAtDesc(name);
        return personalReservations.stream().map(GetPersonalReservationByNameResponse::toDto).toList();
    }
}
