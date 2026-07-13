package com.project.ds_helper.domain.user.service;

import com.project.ds_helper.common.util.PasswordUtil;
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
import com.project.ds_helper.domain.post.util.ImageCompressionUtil;
import com.project.ds_helper.domain.post.util.ImageUtil;
import com.project.ds_helper.domain.post.util.S3Util;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordUtil passwordUtil;
    private final S3Util s3Util;
    private final ImageCompressionUtil imageCompressionUtil;
    private final ImageUtil imageUtil;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserProfileService userProfileService;
    private final UserAuthTokenService userAuthTokenService;
    private final com.project.ds_helper.common.util.UserUtil userUtil;

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

        userAuthTokenService.generateJwtTokenAndPutInResponseHeader(httpServletResponse, userId, role, type);
    }


    /**
     * 로그인 여부 확인
     * **/
    @Transactional(readOnly = true)
    public Object checkIfUserLoggedIn(String refreshToken) {
        return userAuthTokenService.checkIfUserLoggedIn(refreshToken);
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
        userAuthTokenService.generateJwtTokenAndPutInResponseHeader(httpServletResponse, userId, userRole, userType);
    }

    public UserIdentifierResponseDto getMyIdentifier(Authentication authentication) {

        return new UserIdentifierResponseDto(userUtil.extractUserId(authentication), getCurrentUserRole(authentication));
    }

    public UserGetSelfInfoAtMyPageResponseDto getMyInfo(Authentication authentication) {
        return userProfileService.getMyInfo(authentication);
    }

    public UserGetSelfInfoAtMyPageResponseDto updateMyInfo(Authentication authentication, UpdateMyInfoRequestDto dto, MultipartFile profileImage) throws Exception {
        return userProfileService.updateMyInfo(authentication, dto, profileImage);
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

