package com.project.ds_helper.domain.user.service;


import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.user.dto.request.MobileNaverLoginRequestDto;
import com.project.ds_helper.domain.user.entity.NaverOauth;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserType;
import com.project.ds_helper.domain.user.repository.NaverOauthRepository;
import com.project.ds_helper.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class MobileNaverOauthService {

    @Value("${naver.client.id}")
    private String clientId;

    @Value("${naver.client.secret}")
    private String clientSecret;

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final CookieUtil cookieUtil;
    private final RestTemplate restTemplate;
    private final UserUtil userUtil;
    private final UserRepository userRepository;
    private final NaverOauthRepository naverOauthRepository;
    private final UserLoginHistoryService userLoginHistoryService;


    public MobileNaverOauthService(JwtUtil jwtUtil, StringRedisTemplate stringRedisTemplate, CookieUtil cookieUtil, @Qualifier(value = "customRestTemplate") RestTemplate restTemplate, UserUtil userUtil, UserRepository userRepository, NaverOauthRepository naverOauthRepository, UserLoginHistoryService userLoginHistoryService) {
        this.jwtUtil = jwtUtil;
        this.stringRedisTemplate = stringRedisTemplate;
        this.cookieUtil = cookieUtil;
        this.restTemplate = restTemplate;
        this.userUtil = userUtil;
        this.userRepository = userRepository;
        this.naverOauthRepository = naverOauthRepository;
        this.userLoginHistoryService = userLoginHistoryService;
    }


    public JwtResponse mobileNaverLogin(MobileNaverLoginRequestDto dto, HttpServletResponse httpServletResponse) {

        Map<String, Object> naverUserInfo = fetchUserInfoFromNaver(dto.accessToken());

        String socialOauthId = (String) naverUserInfo.get("id");
        String profileImageUrl = (String) naverUserInfo.get("profile_image");
//        String ageRange = (String) naverUserInfo.get("age");
        String gender = (String) naverUserInfo.get("gender");

        String phoneNumber = (String) naverUserInfo.get("mobile");
        String birthyear = (String) naverUserInfo.get("birthyear");
        // kakao(male, female) 과 naver(M, W) 불일치로 변환
        gender = convertNaverGender(gender);
        String email = (String) naverUserInfo.get("email");
        if (email != null) {
            email = email.trim();
            if (email.isEmpty()) {
                email = null;
            }
        }
        String name = (String) naverUserInfo.get("name");
        String token = (String) naverUserInfo.get("token");
        log.debug("socialOauthId : {}, profileImageUril : {}, gender : {}, phoneNumber : {}, birthyear : {},email : {}, name : {}, token : {}", socialOauthId, profileImageUrl, gender, phoneNumber, birthyear, email, name,token);

        // 네이버 회원가입 유무에 따른 처리 (socialOauthId, email)
        Optional<NaverOauth> optionalNaverOauth = naverOauthRepository.findBySocialOauthIdAndOauthEmail(socialOauthId, email);
        log.debug("optionalNaverOauth selected");
        // 기 가입된 유저 토큰 반환 처리
        if(optionalNaverOauth.isPresent()){
            log.debug("Already Joined Naver Oauth User");

            // 토큰 발급을 위한 userId, userRole 획득
            NaverOauth naverOauth = optionalNaverOauth.get();
            naverOauth.updateRefreshToken(dto.refreshToken());
            //if (naverOauth.getUser().isDeleted()) {
                //throw new IllegalArgumentException("Deleted User");
            //}
            String userId = naverOauth.getUser().getId();
            String userRole = naverOauth.getUser().getRole().name();
            String userType = naverOauth.getUser().getType().name();
            log.debug("userId : {}, userRole : {}", userId, userRole);


            String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
            String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
            userLoginHistoryService.recordSuccessfulLogin(naverOauth.getUser());
            saveRefreshTokenWithTtl(userId, refreshToken);
            return new JwtResponse(accessToken, refreshToken);

        }else {
            // provider와 무관하게 User 테이블 기준 이메일 중복 가입을 차단한다.
            if (email != null && userRepository.existsByEmail(email)) {
                log.debug("mobile naver signup rejected. duplicated email found in user table. email={}", email);
                throw new IllegalArgumentException("Email Already Exist");
            }
            // Email 기반 로컬 회원가입 여부 조회
            // 로컬 회원이 있다면 KakaoOauth에 매핑
            // 추후 로직 추가

            // 로컬 회원가입 미가입 신규 회원가입 처리
            User newUser = User.builder()
                    .email(email)
                    .name(name)
                    .profileImageUrl(profileImageUrl)
                    .gender(gender)
                    .birthyear(birthyear)
                    .phoneNumber(phoneNumber)
                    .type(UserType.PERSONAL)
                    .naverOauthConnected(true)
                    .build();
            log.debug("newUser is built");

            // 신규 유저 저장
            User user = userRepository.save(newUser);
            log.debug("user saved");

            // KakaoOauth 빌드
            NaverOauth naverOauth = NaverOauth.builder()
                    .user(user)
                    .socialOauthId(socialOauthId)
                    .oauthEmail(email)
                    .refreshToken(dto.refreshToken())
                    .build();
            log.debug("naverOauth is built");

            // kakaoOauth 저장
            naverOauthRepository.save(naverOauth);
            log.debug("naverOauth is saved");

            // jwt token 발급
            String userId = user.getId();
            String userRole = user.getRole().name();
            String userType = user.getType().name();
            log.debug("userId : {}. userRole : {}, userType : {}", userId, userRole, userType);

            String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
            String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
            userLoginHistoryService.recordSuccessfulLogin(user);
            saveRefreshTokenWithTtl(userId, refreshToken);
            return new JwtResponse(accessToken, refreshToken);
        }
    }

    public Map<String, Object> fetchUserInfoFromNaver(String accessToken){
        // Step 3: 사용자 정보 요청
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> profileResponse = restTemplate.exchange(
                "https://openapi.naver.com/v1/nid/me",
                HttpMethod.GET,
                request,
                Map.class
        );

        Map<String, Object> responseBody = profileResponse.getBody();
        assert responseBody != null;
        return (Map<String, Object>) responseBody.get("response");
    }

    private String convertNaverGender(String gender) {
        if (gender == null || gender.isBlank()) {
            return null;
        }
        return gender.equalsIgnoreCase("M") ? "male" : "female";
    }


    /**
     * jwt token 발급 후 쿠키에 저장하는 메소드
     * **/
    public void generateJwtTokenAndPutInCookie(HttpServletResponse httpServletResponse, String userId, String userRole, String userType){
        // jwt 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
        String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
        log.debug("accessToken : {}, refreshToken : {}", accessToken, refreshToken);

        saveRefreshTokenWithTtl(userId, refreshToken);

        // 쿠키에 토큰 추가
        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.addAccessTokenCookie(accessToken).toString());
        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.addRefreshTokenCookie(refreshToken).toString());
        log.debug("jwt token is put in cookie");
    }

    private void saveRefreshTokenWithTtl(String userId, String refreshToken) {
        stringRedisTemplate.opsForValue().set(
                jwtUtil.toRedisRefreshTokenKey(userId),
                refreshToken,
                Duration.ofMillis(jwtUtil.getRefreshTokenExpirationTime())
        );
        log.debug("RefreshToken Saved. UserId : {}, key : {}", userId, jwtUtil.toRedisRefreshTokenKey(userId));
    }
}

