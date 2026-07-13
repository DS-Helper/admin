package com.project.ds_helper.domain.user.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.user.dto.request.UpdateMyInfoRequestDto;
import com.project.ds_helper.domain.user.dto.response.UserGetSelfInfoAtMyPageResponseDto;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.post.util.ImageCompressionUtil;
import com.project.ds_helper.domain.post.util.ImageUtil;
import com.project.ds_helper.domain.post.util.S3Util;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserUtil userUtil;
    private final S3Util s3Util;
    private final ImageCompressionUtil imageCompressionUtil;
    private final ImageUtil imageUtil;

    @Transactional(readOnly = true)
    public UserGetSelfInfoAtMyPageResponseDto getMyInfo(Authentication authentication) {
        User user = userUtil.findUserById(userUtil.extractUserId(authentication));
        return UserGetSelfInfoAtMyPageResponseDto.toDto(user);
    }

    @Transactional
    public UserGetSelfInfoAtMyPageResponseDto updateMyInfo(Authentication authentication, UpdateMyInfoRequestDto dto, MultipartFile profileImage) throws Exception {
        User user = userUtil.findUserById(userUtil.extractUserId(authentication));

        String email = dto.getEmail();
        if (email != null && !email.isBlank() && !email.equals(user.getEmail()) && userUtil.existsByEmail(email)) {
            throw new BadRequestException("Email Already Exist");
        }

        if (dto.isRemoveProfileImage() && profileImage != null && !profileImage.isEmpty()) {
            throw new BadRequestException("Profile image remove and upload cannot be requested together");
        }

        patchUserInfo(dto, user, profileImage);
        return UserGetSelfInfoAtMyPageResponseDto.toDto(user);
    }

    private void patchUserInfo(UpdateMyInfoRequestDto dto, User user, MultipartFile profileImage) throws Exception {
        if (dto.getName() != null && !dto.getName().isBlank()) {
            user.setName(dto.getName());
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getBirthyear() != null && !dto.getBirthyear().isBlank()) {
            user.setBirthyear(dto.getBirthyear());
        }
        if (dto.getGender() != null && !dto.getGender().isBlank()) {
            user.setGender(dto.getGender());
        }
        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().isBlank()) {
            user.setPhoneNumber(dto.getPhoneNumber());
        }
        if (dto.isRemoveProfileImage() || (profileImage != null && !profileImage.isEmpty())) {
            user.setProfileImageUrl(resolveProfileImageUrl(user, dto, profileImage));
        }
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
}
