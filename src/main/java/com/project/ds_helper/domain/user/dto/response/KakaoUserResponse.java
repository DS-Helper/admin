package com.project.ds_helper.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.context.annotation.Profile;

// 유저 정보 응답
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "카카오 사용자 정보 응답 DTO", example = "{\"id\":1234567890,\"kakao_account\":{\"email\":\"user@kakao.com\",\"name\":\"홍길동\",\"gender\":\"male\",\"age_range\":\"20~29\",\"birthday\":\"0323\",\"birthyear\":\"1998\",\"phone_number\":\"010-1234-5678\"}}")
public class KakaoUserResponse {

    @NotNull
    @JsonProperty(value = "id")
    private Long socialOauthId;

    @Nullable
    @JsonProperty(value = "kakao_account")
    private KakaoAccount kakaoAccount;
//    @JsonProperty(value = "for_partner")
//    private Partner partner;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoAccount {
        @Nullable
        private Profile profile; // 프로필
        @Nullable
        private String email;
        @Nullable
        private String name;
        @Nullable
        private String gender;
        @Nullable
        @JsonProperty(value = "age_range")
        private String ageRange;
        @Nullable
        private String birthday;
        @Nullable
        private String birthyear;
        @Nullable
        @JsonProperty(value = "phone_number")
        private String phoneNumber;
        @Nullable
        @JsonProperty(value = "plusfriends")
        private String plusFriends;



        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Profile {
//            private String nickname;
            @Nullable
            @JsonProperty(value = "profile_image_url")
            private String profileImageUrl;
        }

    }

//    @Data
//    public static class Partner {
//        @JsonProperty(value = "uuid")
//        private String uuid;
//    }
}
