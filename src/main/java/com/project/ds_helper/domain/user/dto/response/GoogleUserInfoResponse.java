package com.project.ds_helper.domain.user.dto.response;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
@Schema(description = "구글 사용자 정보 응답 DTO", example = "{\"id\":\"117200000000000000000\",\"email\":\"user@gmail.com\",\"verified_email\":true,\"picture\":\"https://lh3.googleusercontent.com/a/profile-image\"}")
public class GoogleUserInfoResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("email")
    private String email;

    @JsonProperty("verified_email")
    private Boolean verifiedEmail;

    @JsonProperty("picture")
    private String picture;
}

