package com.project.ds_helper.domain.user.service;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.domain.user.dto.request.MobileKakaoLoginRequestDto;
import com.project.ds_helper.domain.user.dto.response.KakaoTokenResponse;
import com.project.ds_helper.domain.user.dto.response.KakaoUserResponse;
import com.project.ds_helper.domain.user.entity.KakaoOauth;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserType;
import com.project.ds_helper.domain.user.repository.KakaoOauthRepository;
import com.project.ds_helper.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class MobileKakaoOauthService {

    @Value("${kakao.client.id}")
    private String clientId ;
    @Value("${kakao.client.secret}")
    String clientSecret;

    private final WebClient kakaoOauthWebClient;
    private final WebClient kakaoApiWebClient;
    private final KakaoOauthRepository kakaoOauthRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;
    private final CookieUtil cookieUtil;
    private final StringRedisTemplate stringRedisTemplate;

    public MobileKakaoOauthService(@Qualifier("kakaoOauthWebClient") WebClient kakaoOauthWebClient,
            @Qualifier("kakaoApiWebClient") WebClient kakaoApiWebClient,
            RestTemplate restTemplate,
            JwtUtil jwtUtil, CookieUtil cookieUtil,
            KakaoOauthRepository kakaoOauthRepository,
            UserRepository userRepository,
            @Qualifier("CustomStringRedisTemplate") StringRedisTemplate stringRedisTemplate){
        this.kakaoOauthWebClient = kakaoOauthWebClient;
        this.kakaoApiWebClient = kakaoApiWebClient;
        this.restTemplate = restTemplate;
        this.jwtUtil = jwtUtil;
        this.cookieUtil = cookieUtil;
        this.kakaoOauthRepository = kakaoOauthRepository;
        this.userRepository = userRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }



    /**
     * 모바일 카카오 로그인
     * 
     * OauthType provider,
     * String accessToken을 받아 회원가입 or 로그인 진행
     * **/
    public JwtResponse mobileKakaoLogin(
            MobileKakaoLoginRequestDto dto,
            HttpServletResponse httpServletResponse
    ) throws IOException {
        // 토큰에서 유저 정보 조회 (비동기로 시작 -> 동기화)
        KakaoUserResponse kakaoUserResponse = fetchUserInfo(dto.accessToken());
        log.debug("kakao user responded successfully");

        // 유저 정보 추출
        Long socialOauthId = kakaoUserResponse.getSocialOauthId();

        if(kakaoUserResponse.getKakaoAccount() == null){
            throw new IOException("No Kakao Account");
        }

        String email = kakaoUserResponse.getKakaoAccount().getEmail();
        if (email != null) {
            email = email.trim();
            if (email.isEmpty()) {
                email = null;
            }
        }
        String name = kakaoUserResponse.getKakaoAccount().getName();
        String gender =  kakaoUserResponse.getKakaoAccount().getGender();
        String birthyear = kakaoUserResponse.getKakaoAccount().getBirthyear();

        String profileImageUrl = "";
        String phoneNumber = "";
        if(kakaoUserResponse.getKakaoAccount().getProfile() != null){
            profileImageUrl = kakaoUserResponse.getKakaoAccount().getProfile().getProfileImageUrl();
            phoneNumber = kakaoUserResponse.getKakaoAccount().getPhoneNumber();
        }
        log.debug("socialOauthId : {}, email : {}, name : {}, profileImageUrl : {}, gender : {},birthyear : {}, profileImageUrl : {}, phoneNumber : {}"
                , socialOauthId, email, name, profileImageUrl, gender, birthyear, profileImageUrl, phoneNumber);

        // 카카오 회원가입 유무에 따른 처리 (socialOauthId, email)
        Optional<KakaoOauth> optionalKakaoOauth = kakaoOauthRepository.findBySocialOauthIdAndOauthEmail(socialOauthId, email);
        log.debug("optionalKakaoOauth selected");
        // 기 가입된 유저 토큰 반환 처리
        if(optionalKakaoOauth.isPresent()){
            log.debug("Already Joined Kakao Oauth User");

            // 토큰 발급을 위한 userId, userRole 획득
            KakaoOauth kakaoOauth = optionalKakaoOauth.get();
            //if (kakaoOauth.getUser().isDeleted()) {
                //throw new IllegalArgumentException("Deleted User");
            //}
            String userId = kakaoOauth.getUser().getId();
            String userRole = kakaoOauth.getUser().getRole().name();
            String userType = kakaoOauth.getUser().getType().name();
            log.debug("userId : {}, userRole : {}", userId, userRole);

            String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
            String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
            saveRefreshTokenWithTtl(userId, refreshToken);
            return new JwtResponse(accessToken, refreshToken);

        }else {
            // provider와 무관하게 User 테이블 기준 이메일 중복 가입을 차단한다.
            if (email != null && userRepository.existsByEmail(email)) {
                log.debug("mobile kakao signup rejected. duplicated email found in user table. email={}", email);
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
                    .kakaoOauthConnected(true)
                    .build();
            log.debug("newUser is built");

            // 신규 유저 저장
            User user = userRepository.save(newUser);
            log.debug("user saved");

            // KakaoOauth 빌드
            KakaoOauth kakaoOauth = KakaoOauth.builder()
                    .user(user)
                    .socialOauthId(socialOauthId)
                    .oauthEmail(email)
                    .build();
            log.debug("kakaoOauth is built");

            // kakaoOauth 저장
            kakaoOauthRepository.save(kakaoOauth);
            log.debug("kakaoOauth is saved");

            // jwt token 발급
            String userId = user.getId();
            String userRole = user.getRole().name();
            String userType = user.getType().name();
            log.debug("userId : {}. userRole : {}, userType : {}", userId, userRole, userType);

            // jwt 토큰 발급 및 쿠키 저장
            String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
            String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
            saveRefreshTokenWithTtl(userId, refreshToken);
            return new JwtResponse(accessToken, refreshToken);
        }
    }

    /**
     * 2) 받은 액세스 토큰으로 사용자 정보 조회
     */
    public KakaoUserResponse fetchUserInfo(String accessToken) { // Mono<KakaoUserResponse>
        return kakaoApiWebClient.get()
                .uri("/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(KakaoUserResponse.class)
                .block(Duration.ofMillis(5000));
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

