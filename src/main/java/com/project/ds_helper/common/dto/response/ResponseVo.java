package com.project.ds_helper.common.dto.response;

import com.project.ds_helper.common.enums.SuccessCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Schema(
        description = "Common API response wrapper DTO"
//        example = "{\"success\":true,\"code\":{\"code\":\"OK\",\"message\":\"Request succeeded\",\"httpStatus\":\"OK\"},\"message\":\"Request succeeded\",\"data\":null}"
)
public class ResponseVo<T> {
    @Schema(description = "요청 성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 코드 정보")
    private ResponseCodeInfoDto code;

    @Schema(description = "응답 메시지. code.message와 동일한 값이 내려갑니다.", example = "요청이 성공했습니다.")
    private String message;

    @Schema(description = "실제 응답 데이터")
    private T data;

    public ResponseVo(boolean success, ResponseCode code, String message, T data) {
        this.success = success;
        this.code = ResponseCodeInfoDto.from(code);
        this.message = message;
        this.data = data;
    }

    public static ResponseVo OkWithNullData() {
        return new ResponseVo(true, SuccessCode.OK, SuccessCode.OK.getMessage(), null);
    }

    public static ResponseVo error(ResponseCode code, String message) {
        return new ResponseVo(false, code, message, null);
    }
}
