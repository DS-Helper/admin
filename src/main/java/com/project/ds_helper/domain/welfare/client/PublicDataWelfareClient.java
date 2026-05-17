package com.project.ds_helper.domain.welfare.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.project.ds_helper.domain.welfare.dto.external.WelfareApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import java.time.Duration;

/**
 * 공공데이터포털 복지 서비스 API 통신 클라이언트.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PublicDataWelfareClient {

    private final WebClient publicDataWebClient;
    private final String publicDataServiceKey;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final XmlMapper xmlMapper = new XmlMapper();

    /**
     * 지자체 복지 서비스 목록을 조회한다.
     * 실제 공공데이터 응답은 XML(wantedList)로 내려올 수 있어 JSON/XML을 모두 파싱한다.
     */
    public WelfareApiResponse fetchWelfareList(int pageNumber, int numberOfRows, String cityProvinceName, String districtName) {
        log.info("PublicDataWelfareClient.fetchWelfareList started. pageNumber={}, cityProvinceName={}, districtName={}",
                pageNumber, cityProvinceName, districtName);

        String responseBody = publicDataWebClient.get()
                .uri(uriBuilder -> buildWelfareListUri(uriBuilder, pageNumber, numberOfRows, cityProvinceName, districtName))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .block();

        return parseWelfareListResponse(responseBody);
    }

    public WelfareApiResponse fetchWelfareList(int pageNumber, int numberOfRows) {
        return fetchWelfareList(pageNumber, numberOfRows, null, null);
    }

    /**
     * 특정 복지 서비스의 상세 정보를 조회한다.
     * 상세 응답은 원문 구조 보존이 필요하므로 String으로 반환하고 Service에서 필요한 필드를 추출한다.
     */
    public String fetchWelfareDetail(String serviceId) {
        log.info("PublicDataWelfareClient.fetchWelfareDetail started. serviceId={}", serviceId);

        return publicDataWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/LcgvWelfaredetailed")
                        .queryParam("serviceKey", publicDataServiceKey)
                        .queryParam("servId", serviceId)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .block();
    }

    private WelfareApiResponse parseWelfareListResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }

        try {
            String trimmedBody = responseBody.trim();
            if (trimmedBody.startsWith("<")) {
                return xmlMapper.readValue(trimmedBody, WelfareApiResponse.class);
            }
            return objectMapper.readValue(trimmedBody, WelfareApiResponse.class);
        } catch (Exception e) {
            log.error("PublicDataWelfareClient.parseWelfareListResponse failed. responsePrefix={}",
                    responseBody.substring(0, Math.min(responseBody.length(), 200)), e);
            throw new IllegalStateException("Failed to parse welfare list response", e);
        }
    }

    private java.net.URI buildWelfareListUri(UriBuilder uriBuilder, int pageNumber, int numberOfRows, String cityProvinceName, String districtName) {
        UriBuilder builder = uriBuilder
                .path("/LcgvWelfarelist")
                .queryParam("serviceKey", publicDataServiceKey)
                .queryParam("pageNo", pageNumber)
                .queryParam("numOfRows", numberOfRows);

        // 전체 동기화 시에는 지역 파라미터를 보내지 않아야 공공데이터포털 전체 목록 기준으로 종료 서비스를 판단할 수 있다.
        if (StringUtils.hasText(cityProvinceName)) {
            builder.queryParam("ctpvNm", cityProvinceName);
        }
        if (StringUtils.hasText(districtName)) {
            builder.queryParam("sggNm", districtName);
        }

        return builder.build();
    }
}
