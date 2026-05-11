package com.project.ds_helper.domain.user.dto.response;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
@Schema(description = "구글 토큰 응답 DTO", example = "{\"access_token\":\"ya29.a0Af...\",\"expires_in\":3599,\"scope\":\"openid email profile\",\"token_type\":\"Bearer\",\"id_token\":\"eyJhbGciOi...\"}")
public class GoogleTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
    private Integer expiresIn;

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("id_token")
    private String idToken;
}


