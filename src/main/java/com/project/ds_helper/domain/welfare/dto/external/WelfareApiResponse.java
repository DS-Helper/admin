package com.project.ds_helper.domain.welfare.dto.external;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 공공데이터포털 복지서비스 API 외부 응답 DTO
 */
@Getter
@NoArgsConstructor
@JacksonXmlRootElement(localName = "wantedList")
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
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<WelfareApiItem> serviceList;
}
