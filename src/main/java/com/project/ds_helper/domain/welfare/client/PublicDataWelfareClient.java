package com.project.ds_helper.domain.welfare.client;

import com.project.ds_helper.domain.welfare.dto.external.WelfareApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * 공공데이터포털 복지서비스 API 통신 클라이언트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PublicDataWelfareClient {

    private final WebClient publicDataWebClient;
    private final String publicDataServiceKey;

    /**
     * 지자체 복지서비스 목록을 조회합니다.
     * 
     * @param pageNumber 페이지 번호
     * @param numberOfRows 한 페이지 결과 수
     * @param cityProvinceName 시도명
     * @param districtName 시군구명
     * @return 외부 API 응답 DTO
     */
    public WelfareApiResponse fetchWelfareList(int pageNumber, int numberOfRows, String cityProvinceName, String districtName) {
        log.info("공공데이터 API 호출 시작: pageNumber={}, cityProvinceName={}, districtName={}", pageNumber, cityProvinceName, districtName);

        return publicDataWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/LcgvWelfarelist")
                        .queryParam("serviceKey", publicDataServiceKey)
                        .queryParam("pageNo", pageNumber)
                        .queryParam("numOfRows", numberOfRows)
                        .queryParam("ctpvNm", cityProvinceName)
                        .queryParam("sggNm", districtName)
                        .queryParam("_type", "json") // JSON 형식 요청
                        .build())
                .retrieve()
                .bodyToMono(WelfareApiResponse.class)
                .timeout(Duration.ofSeconds(10)) // 10초 타임아웃
                .block();
    }

    /**
     * 특정 복지서비스의 상세 정보를 조회합니다.
     * (상세 API 대응용)
     */
    public String fetchWelfareDetail(String serviceId) {
        log.info("공공데이터 상세 API 호출 시작: serviceId={}", serviceId);

        return publicDataWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/LcgvWelfaredetailed")
                        .queryParam("serviceKey", publicDataServiceKey)
                        .queryParam("servId", serviceId)
                        .queryParam("_type", "json")
                        .build())
                .retrieve()
                .bodyToMono(String.class) // 상세 API 구조가 복잡할 수 있어 우선 String으로 받음
                .timeout(Duration.ofSeconds(10))
                .block();
    }
}
