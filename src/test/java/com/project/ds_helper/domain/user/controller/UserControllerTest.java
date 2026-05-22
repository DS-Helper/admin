package com.project.ds_helper.domain.user.controller;

import com.project.ds_helper.domain.user.dto.request.OauthWithdrawRequestDto;
import com.project.ds_helper.domain.user.dto.request.UpdateMyInfoRequestDto;
import com.project.ds_helper.domain.user.dto.response.UserGetSelfInfoAtMyPageResponseDto;
import com.project.ds_helper.domain.user.dto.response.UserIdentifierResponseDto;
import com.project.ds_helper.domain.user.dto.response.WithdrawUserResponseDto;
import com.project.ds_helper.domain.user.service.GoogleOAuthService;
import com.project.ds_helper.domain.user.service.KakaoOauthService;
import com.project.ds_helper.domain.user.service.NaverOauthService;
import com.project.ds_helper.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private KakaoOauthService kakaoOauthService;

    @Mock
    private GoogleOAuthService googleOAuthService;

    @Mock
    private NaverOauthService naverOauthService;

    @Mock
    private Authentication authentication;

    @Mock
    private MultipartFile profileImage;

    @InjectMocks
    private UserController userController;

    @Test
    @DisplayName("내 정보 조회는 DTO 응답을 반환한다")
    void getMyInfo_returnsDtoResponse() {
        UserGetSelfInfoAtMyPageResponseDto dto =
                new UserGetSelfInfoAtMyPageResponseDto("홍길동", "user@test.com", "1990", "male", "010-1111-1111", "profile");
        when(userService.getMyInfo(authentication)).thenReturn(dto);

        ResponseEntity<UserGetSelfInfoAtMyPageResponseDto> response = userController.getMyInfo(authentication);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("현재 유저 식별자 조회는 DTO 응답을 반환한다")
    void getMyIdentifier_returnsDtoResponse() {
        UserIdentifierResponseDto dto = new UserIdentifierResponseDto("user-1", "USER");
        when(userService.getMyIdentifier(authentication)).thenReturn(dto);

        ResponseEntity<UserIdentifierResponseDto> response = userController.getMyIdentifier(authentication);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("내 정보 수정은 수정된 DTO 응답을 반환한다")
    void updateMyInfo_returnsUpdatedResponse() throws Exception {
        UpdateMyInfoRequestDto requestDto = new UpdateMyInfoRequestDto();
        ReflectionTestUtils.setField(requestDto, "name", "홍길동");
        ReflectionTestUtils.setField(requestDto, "email", "user@test.com");
        ReflectionTestUtils.setField(requestDto, "birthyear", "1998");
        ReflectionTestUtils.setField(requestDto, "gender", "male");
        ReflectionTestUtils.setField(requestDto, "phoneNumber", "010-1234-5678");
        ReflectionTestUtils.setField(requestDto, "removeProfileImage", false);

        UserGetSelfInfoAtMyPageResponseDto dto =
                new UserGetSelfInfoAtMyPageResponseDto("홍길동", "user@test.com", "1998", "male", "010-1234-5678", "profile");
        when(userService.updateMyInfo(authentication, requestDto, profileImage)).thenReturn(dto);

        ResponseEntity<UserGetSelfInfoAtMyPageResponseDto> response =
                userController.updateMyInfo(authentication, requestDto, profileImage);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("카카오 OAuth 회원 탈퇴는 서비스에 위임한다")
    void withdrawKakaoOauthUser_delegatesToService() {
        OauthWithdrawRequestDto dto = new OauthWithdrawRequestDto("kakao-access-token");
        WithdrawUserResponseDto responseDto = new WithdrawUserResponseDto("user-1", LocalDateTime.of(2026, 5, 21, 14, 30));
        when(kakaoOauthService.withdraw(authentication, dto)).thenReturn(responseDto);

        ResponseEntity<WithdrawUserResponseDto> response = userController.withdrawKakaoOauthUser(authentication, dto);

        verify(kakaoOauthService).withdraw(authentication, dto);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(responseDto);
    }

    @Test
    @DisplayName("구글 OAuth 회원 탈퇴는 서비스에 위임한다")
    void withdrawGoogleOauthUser_delegatesToService() {
        OauthWithdrawRequestDto dto = new OauthWithdrawRequestDto("google-access-token");
        WithdrawUserResponseDto responseDto = new WithdrawUserResponseDto("user-1", LocalDateTime.of(2026, 5, 21, 14, 30));
        when(googleOAuthService.withdraw(authentication, dto)).thenReturn(responseDto);

        ResponseEntity<WithdrawUserResponseDto> response = userController.withdrawGoogleOauthUser(authentication, dto);

        verify(googleOAuthService).withdraw(authentication, dto);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(responseDto);
    }

    @Test
    @DisplayName("네이버 OAuth 회원 탈퇴는 서비스에 위임한다")
    void withdrawNaverOauthUser_delegatesToService() {
        OauthWithdrawRequestDto dto = new OauthWithdrawRequestDto("naver-access-token");
        WithdrawUserResponseDto responseDto = new WithdrawUserResponseDto("user-1", LocalDateTime.of(2026, 5, 21, 14, 30));
        when(naverOauthService.withdraw(authentication, dto)).thenReturn(responseDto);

        ResponseEntity<WithdrawUserResponseDto> response = userController.withdrawNaverOauthUser(authentication, dto);

        verify(naverOauthService).withdraw(authentication, dto);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(responseDto);
    }
}
