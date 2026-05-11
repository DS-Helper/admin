package com.project.ds_helper.domain.user.service;

import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
import com.project.ds_helper.common.enums.JwtTokenType;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.common.util.PasswordUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.post.util.ImageCompressionUtil;
import com.project.ds_helper.domain.post.util.ImageUtil;
import com.project.ds_helper.domain.post.util.S3Util;
import com.project.ds_helper.domain.user.dto.request.OrganizationJoinReqDto;
import com.project.ds_helper.domain.user.dto.request.OrganizationLoginReqDto;
import com.project.ds_helper.domain.user.dto.request.UpdateMyInfoRequestDto;
import com.project.ds_helper.domain.user.dto.request.UserJoinReqDto;
import com.project.ds_helper.domain.user.dto.response.UserIdentifierResponseDto;
import com.project.ds_helper.domain.user.dto.response.UserGetSelfInfoAtMyPageResponseDto;
import com.project.ds_helper.domain.user.entity.Organization;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserType;
import com.project.ds_helper.domain.user.repository.OrganizationRepository;
import com.project.ds_helper.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordUtil passwordUtil;
    private final S3Util s3Util;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final ImageCompressionUtil imageCompressionUtil;
    private final ImageUtil imageUtil;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserUtil userUtil;

    /**
     * 기관 회원가입
     * **/
    @Transactional
    public void organizationJoin(OrganizationJoinReqDto dto, List<MultipartFile> certifications) throws IOException {

        String email = dto.getEmail();
        String password = dto.getPassword();
        String passwordCheck = dto.getPasswordCheck();
        log.debug("");

        if(!passwordUtil.isPasswordMatch(password, passwordCheck)){throw new IllegalArgumentException("Password Not Match");}

        // 로컬 회원가입 미가입 신규 회원가입 처리
        User newUser = User.builder()
                .email(email)
                .password(bCryptPasswordEncoder.encode(password))
                .name(dto.getOrganizationName())
                .type(UserType.ORGANIZATION)
                .build();
        log.debug("newUser is built");

        // 신규 유저 저장
        User user = userRepository.save(newUser);
        log.debug("user saved");

        // Organization 미리 세팅
        Organization organization;
        
        // 이미지 업로드 후 url 반환
        if(certifications != null) {
            List<String> imageUrls = s3Util.uploadImages(certifications.stream().map(certification -> {
                try {
                    return imageCompressionUtil.compressImage(certification, imageUtil.toStoredFilename());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).toList());

            // 기관 엔티티 빌드
            organization = Organization.builder()
                    .organizationName(dto.getOrganizationName())
                    .organizationPhoneNumber(dto.getOrganizationPhoneNumber())
                    .certificationUrl(imageUrls)
                    .userId(user.getId())
                    .build();
        }else{
            // 기관 엔티티 빌드
            organization = Organization.builder()
                    .organizationName(dto.getOrganizationName())
                    .organizationPhoneNumber(dto.getOrganizationPhoneNumber())
                    .userId(user.getId())
                    .build();
        }

        organizationRepository.save(organization);
        log.debug("Organization Saved Successfully");
    }

    /**
     * 기관 로그인
     * **/
    @Transactional(readOnly = true)
    public void organizationLogin(OrganizationLoginReqDto dto, HttpServletResponse httpServletResponse) {

        log.debug("Organization Login Start");

        String email = dto.getEmail();
        String password = dto.getPassword();
        log.debug("email : {}, password : {}", email, password);

        User user = userRepository.findByEmail(email).orElseThrow(()-> new RuntimeException("Organization Not Found"));
        if (user.isDeleted()) {
            throw new RuntimeException("Deleted User");
        }
        if(user.getType() != UserType.ORGANIZATION){ throw new RuntimeException("Not Organization"); }

        if(!bCryptPasswordEncoder.matches(password, user.getPassword())){throw new IllegalArgumentException("Wrong Password");}

        String userId = user.getId();
        String role = user.getRole().name();
        String type = user.getType().name();
        log.debug("userId : {}, role : {}, type : {}", userId, role, type);

        generateJwtTokenAndPutInResponseHeader(httpServletResponse, userId, role, type);
    }


    /**
     * 로그인 여부 확인
     * **/
    @Transactional(readOnly = true)
    public Object checkIfUserLoggedIn(String refreshToken) {
        if(refreshToken == null || refreshToken.isBlank()) return false; // null 이거나 공백이면 false 반환
        if(jwtUtil.isExpired(refreshToken)) return false;
        String userId = jwtUtil.getId(refreshToken);
        log.debug("userID : {}", userId);

        String refreshTokenFromRedis = stringRedisTemplate.opsForValue().get(jwtUtil.toRedisRefreshTokenKey(userId));
        log.debug("refreshTokenFromRedis : {}", refreshTokenFromRedis);
        if(refreshTokenFromRedis == null || refreshTokenFromRedis.isBlank() || jwtUtil.isExpired(refreshTokenFromRedis)) {
            log.debug("refreshToken from redis is null or blank or expired");
            return false;
        }

        return true;
    }
    
    /**
     * 유저 회원가입
     * **/
    @Transactional
    public void userJoin(@Valid UserJoinReqDto dto, HttpServletResponse httpServletResponse) throws BadRequestException {
        String email = dto.getEmail();
        String password = dto.getPassword();
        String passwordCheck = dto.getPasswordCheck();
        log.debug("email : {}, password : {}, passwordCheck : {}", email, password, passwordCheck);
        
        // email 중복 체크
        if(userUtil.existsByEmail(email)){throw new BadRequestException("Email Already Exist"); }
        // passsword 일치 체크
        if(!password.equals(passwordCheck)){throw new BadRequestException("Password Not Matches");}
        // 회원가입
        User user = User.builder()
                .email(email)
                .password(bCryptPasswordEncoder.encode(password))
                .build();
        User savedUser = userRepository.save(user);
        // 로그인까지 바로 진행

        // jwt token 발급
        String userId = user.getId();
        String userRole = user.getRole().name();
        String userType = user.getType().name();
        log.debug("userId : {}. userRole : {}, userType : {}", userId, userRole, userType);

        // jwt 토큰 발급 및 쿠키 저장
        generateJwtTokenAndPutInResponseHeader(httpServletResponse, userId, userRole, userType);



    }

    /**
     * jwt token 발급 후 쿠키에 저장하는 메소드
     * **/
    public void generateJwtTokenAndPutInResponseHeader(HttpServletResponse httpServletResponse, String userId, String userRole, String userType){
        // jwt 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
        String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
        log.debug("accessToken : {}, refreshToken : {}", accessToken, refreshToken);

        stringRedisTemplate.opsForValue().set(jwtUtil.toRedisRefreshTokenKey(userId), refreshToken);
        log.debug("RefreshToken Saved. UserId : {}, key : {}", userId, jwtUtil.toRedisRefreshTokenKey(userId));

        // 쿠키에 토큰 추가
        httpServletResponse.setHeader(JwtTokenType.ACCESS_TOKEN_NAME.getTokenName(), accessToken);
        httpServletResponse.setHeader(JwtTokenType.REFRESH_TOKEN_NAME.getTokenName(), refreshToken);
        log.debug("jwt token is put in header");
    }

    /** 유저가 마이페이지에서 자신의 정보를 조회하는 메소드 **/
    public UserGetSelfInfoAtMyPageResponseDto getMyInfo(Authentication authentication) {
        User user = userUtil.findUserById(userUtil.extractUserId(authentication));
        return UserGetSelfInfoAtMyPageResponseDto.toDto(user);
    }

    public UserIdentifierResponseDto getMyIdentifier(Authentication authentication) {

        return new UserIdentifierResponseDto(userUtil.extractUserId(authentication), getCurrentUserRole(authentication));
    }

    @Transactional
    public UserGetSelfInfoAtMyPageResponseDto updateMyInfo(Authentication authentication,
                                                           UpdateMyInfoRequestDto dto,
                                                           MultipartFile profileImage) throws Exception {
        User user = userUtil.findUserById(userUtil.extractUserId(authentication));

        String email = dto.getEmail();
        log.debug("email : {}", email);
        if (email != null && !email.isBlank() && !email.equals(user.getEmail()) && userUtil.existsByEmail(email)) {
            throw new BadRequestException("Email Already Exist");
        }

        if (dto.isRemoveProfileImage() && profileImage != null && !profileImage.isEmpty()) {
            throw new BadRequestException("Profile image remove and upload cannot be requested together");
        }
    
        // set 작업
        patchUserInfo(dto, user, profileImage);

        return UserGetSelfInfoAtMyPageResponseDto.toDto(user);
    }

    private String resolveProfileImageUrl(User user, UpdateMyInfoRequestDto dto, MultipartFile profileImage) throws Exception {
        String currentProfileImageUrl = user.getProfileImageUrl();

        if (dto.isRemoveProfileImage()) {
            deleteManagedProfileImage(currentProfileImageUrl);
            return null;
        }

        if (profileImage == null || profileImage.isEmpty()) {
            return currentProfileImageUrl;
        }

        String storedFilename = imageUtil.toStoredFilename();
        s3Util.uploadImage(imageCompressionUtil.compressImage(profileImage, storedFilename));

        String uploadedProfileImageUrl = s3Util.toS3UrlByStoredFilename(storedFilename);
        deleteManagedProfileImage(currentProfileImageUrl);
        return uploadedProfileImageUrl;
    }

    private void deleteManagedProfileImage(String profileImageUrl) {
        if (!s3Util.isManagedS3Url(profileImageUrl)) {
            return;
        }

        s3Util.deleteImagesByS3Key(List.of(s3Util.extractS3KeyFromS3Url(profileImageUrl)));
    }

    private void patchUserInfo(UpdateMyInfoRequestDto dto, User user, MultipartFile profileImage) throws Exception {
        if(dto.getName() != null && !dto.getName().isBlank()){
            user.setName(dto.getName());
        }

        if(dto.getEmail() != null && !dto.getEmail().isBlank()){
            user.setEmail(dto.getEmail());
        }

        if(dto.getBirthyear() != null && !dto.getBirthyear().isBlank()){
            user.setBirthyear(dto.getBirthyear());
        }

        if(dto.getGender() != null && !dto.getGender().isBlank()){
            user.setGender(dto.getGender());
        }

        if(dto.getPhoneNumber() != null && !dto.getPhoneNumber().isBlank()){
            user.setPhoneNumber(dto.getPhoneNumber());
        }

        if(dto.isRemoveProfileImage() || (profileImage != null && !profileImage.isEmpty())){
            user.setProfileImageUrl(resolveProfileImageUrl(user, dto, profileImage));
        }
    }

    private String getCurrentUserRole(Authentication authentication) {
        
        if (authentication == null || authentication.getAuthorities() == null) {
            return null;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse(null);
    }
}

