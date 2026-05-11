package com.project.ds_helper.domain.welfare.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 공공데이터포털 복지서비스 API 외부 응답 DTO
 */
@Getter
@NoArgsConstructor
public class WelfareApiResponse {

    @JsonProperty("resultCode")
    private String resultCode;

    @JsonProperty("resultMessage")
    private String resultMessage;

    @JsonProperty("totalCount")
    private Integer totalCount;

    @JsonProperty("pageNo")
    private Integer pageNumber;

    @JsonProperty("numOfRows")
    private Integer numberOfRows;

    @JsonProperty("servList")
    private List<WelfareApiItem> serviceList;
}
