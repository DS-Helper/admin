package com.project.ds_helper.domain.welfare.service;

import com.project.ds_helper.common.util.DiscordWebhookUtil;
import com.project.ds_helper.domain.welfare.client.PublicDataWelfareClient;
import com.project.ds_helper.domain.welfare.dto.external.WelfareApiItem;
import com.project.ds_helper.domain.welfare.dto.external.WelfareApiResponse;
import com.project.ds_helper.domain.welfare.dto.response.WelfareSyncResultResponse;
import com.project.ds_helper.domain.welfare.entity.WelfareServiceEntity;
import com.project.ds_helper.domain.welfare.repository.WelfareServiceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WelfareSyncServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WelfareDetailSyncParser detailSyncParser = new WelfareDetailSyncParser();

    @Mock
    private PublicDataWelfareClient welfareClient;

    @Mock
    private WelfareServiceRepository welfareServiceRepository;

    @Mock
    private DiscordWebhookUtil discordUtil;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private WelfareSyncService welfareSyncService;

    private String detailSampleJson;
    private String detailSampleXml;
    private String detailRealSampleXml;

    @BeforeEach
    void setUp() throws Exception {
        detailSampleJson = new ClassPathResource("welfare/lcgv-welfare-detail-sample.json")
                .getContentAsString(StandardCharsets.UTF_8);
        detailSampleXml = new ClassPathResource("welfare/lcgv-welfare-detail-sample.xml")
                .getContentAsString(StandardCharsets.UTF_8);
        detailRealSampleXml = new ClassPathResource("welfare/lcgv-welfare-detail-real-sample.xml")
                .getContentAsString(StandardCharsets.UTF_8);
        doAnswer(invocation -> {
            java.util.function.Consumer<?> callback = invocation.getArgument(0);
            ((java.util.function.Consumer) callback).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        doAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        }).when(transactionTemplate).execute(any());
        lenient().when(welfareServiceRepository.findAllByActiveTrue()).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("창원시 복지 데이터 동기화 시 API 데이터를 DB에 저장한다")
    void syncChangwonWelfareData_FetchesAndSavesData() {
        // Given
        WelfareApiResponse response = mock(WelfareApiResponse.class);
        WelfareApiItem item = mock(WelfareApiItem.class);
        
        when(item.getServiceId()).thenReturn("serv-123");
        when(item.getServiceName()).thenReturn("신규 복지");
        when(item.getCityProvinceName()).thenReturn("경상남도");
        when(item.getDistrictName()).thenReturn("창원시");
        
        when(response.getServiceList()).thenReturn(Collections.singletonList(item));
        when(response.getTotalCount()).thenReturn(1);
        
        when(welfareClient.fetchWelfareList(anyInt(), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(response);
        when(welfareServiceRepository.findById("serv-123")).thenReturn(Optional.empty());
        when(welfareClient.fetchWelfareDetail("serv-123")).thenReturn(detailSampleJson);

        // When
        WelfareSyncResultResponse result = welfareSyncService.syncChangwonWelfareData();

        // Then
        assertThat(result.totalProcessedCount()).isEqualTo(1);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isZero();
        ArgumentCaptor<WelfareServiceEntity> captor = ArgumentCaptor.forClass(WelfareServiceEntity.class);
        verify(welfareServiceRepository, atLeastOnce()).save(captor.capture());
        WelfareServiceEntity saved = captor.getValue();
        assertThat(saved.getCityProvinceName()).isEqualTo("경상남도");
        assertThat(saved.getDistrictName()).isEqualTo("창원시");
        assertThat(saved.getTargetDetailContent()).isEqualTo("창원시에 거주하는 청년");
        assertThat(saved.getBenefitContent()).isEqualTo("월 생활지원금 지급");
        assertThat(saved.getSelectionCriteriaContent()).isEqualTo("소득 및 거주 요건 충족");
        assertThat(saved.getApplicationMethodList()).isEqualTo("온라인 신청");
        assertThat(saved.getInquiryContactList()).contains("문의처");
        assertThat(saved.getHomepageList()).contains("홈페이지");
        assertThat(saved.getBasisLawList()).contains("근거법");
        assertThat(saved.getBasisFormList()).contains("신청서");
        verify(discordUtil).sendMessage(contains("정상 완료"));
    }

    @Test
    @DisplayName("단건 상세 fallback은 공공데이터포털 상세 응답을 Entity로 저장하고 반환한다")
    void fetchAndSaveWelfareDetailByServiceId_SavesDetailResponse() {
        String detailResponse = """
                {
                  "servId": "WLF00003495",
                  "servNm": "테스트 복지 서비스",
                  "ctpvNm": "대구광역시",
                  "sggNm": "달성군",
                  "servDgst": "요약",
                  "bizChrDeptNm": "복지부서",
                  "trgterIndvdlNmArray": "청년",
                  "lifeNmArray": "청년",
                  "intrsThemaNmArray": "생활지원",
                  "srvPvsnNm": "현금",
                  "inqNum": "053-000-0000",
                  "servDtlLink": "https://example.com/detail",
                  "lastModYmd": "20260517",
                  "sprtTrgtCn": "지원 대상",
                  "alwServCn": "지원 내용",
                  "slctCritCn": "선정 기준",
                  "aplyMtdCn": "신청 방법"
                }
                """;

        when(welfareClient.fetchWelfareDetail("WLF00003495")).thenReturn(detailResponse);
        when(welfareServiceRepository.findById("WLF00003495")).thenReturn(Optional.empty());
        when(welfareServiceRepository.save(any(WelfareServiceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<WelfareServiceEntity> result = welfareSyncService.fetchAndSaveWelfareDetailByServiceId("WLF00003495");

        assertThat(result).isPresent();
        assertThat(result.get().getServiceId()).isEqualTo("WLF00003495");
        assertThat(result.get().getServiceName()).isEqualTo("테스트 복지 서비스");
        assertThat(result.get().getBenefitContent()).isEqualTo("지원 내용");
        assertThat(result.get().getActive()).isTrue();
        verify(welfareClient).fetchWelfareDetail("WLF00003495");
        verify(welfareServiceRepository).save(any(WelfareServiceEntity.class));
    }

    @Test
    @DisplayName("단건 상세 fallback은 상세 응답이 null이면 빈 결과를 반환한다")
    void fetchAndSaveWelfareDetailByServiceId_returnsEmptyWhenDetailResponseIsNull() {
        when(welfareClient.fetchWelfareDetail("NULL_DETAIL")).thenReturn(null);

        Optional<WelfareServiceEntity> result = welfareSyncService.fetchAndSaveWelfareDetailByServiceId("NULL_DETAIL");

        assertThat(result).isEmpty();
        verify(welfareServiceRepository, never()).save(any());
    }

    @Test
    @DisplayName("단건 상세 fallback은 상세 응답이 blank이면 빈 결과를 반환한다")
    void fetchAndSaveWelfareDetailByServiceId_returnsEmptyWhenDetailResponseIsBlank() {
        when(welfareClient.fetchWelfareDetail("BLANK_DETAIL")).thenReturn("   ");

        Optional<WelfareServiceEntity> result = welfareSyncService.fetchAndSaveWelfareDetailByServiceId("BLANK_DETAIL");

        assertThat(result).isEmpty();
        verify(welfareServiceRepository, never()).save(any());
    }

    @Test
    @DisplayName("단건 상세 fallback은 서비스 ID가 없으면 빈 결과를 반환한다")
    void fetchAndSaveWelfareDetailByServiceId_returnsEmptyWhenServiceIdIsMissing() {
        when(welfareClient.fetchWelfareDetail("MISSING_ID")).thenReturn("{\"servNm\":\"서비스명\"}");

        Optional<WelfareServiceEntity> result = welfareSyncService.fetchAndSaveWelfareDetailByServiceId("MISSING_ID");

        assertThat(result).isEmpty();
        verify(welfareServiceRepository, never()).save(any());
    }

    @Test
    @DisplayName("단건 상세 fallback은 서비스명이 없으면 빈 결과를 반환한다")
    void fetchAndSaveWelfareDetailByServiceId_returnsEmptyWhenServiceNameIsMissing() {
        when(welfareClient.fetchWelfareDetail("MISSING_NAME")).thenReturn("{\"servId\":\"MISSING_NAME\"}");

        Optional<WelfareServiceEntity> result = welfareSyncService.fetchAndSaveWelfareDetailByServiceId("MISSING_NAME");

        assertThat(result).isEmpty();
        verify(welfareServiceRepository, never()).save(any());
    }

    @Test
    @DisplayName("단건 상세 fallback은 상세 조회 예외가 발생하면 빈 결과를 반환한다")
    void fetchAndSaveWelfareDetailByServiceId_returnsEmptyWhenDetailFetchFails() {
        when(welfareClient.fetchWelfareDetail("FETCH_FAIL")).thenThrow(new RuntimeException("network error"));

        Optional<WelfareServiceEntity> result = welfareSyncService.fetchAndSaveWelfareDetailByServiceId("FETCH_FAIL");

        assertThat(result).isEmpty();
        verify(welfareServiceRepository, never()).save(any());
    }

    @Test
    @DisplayName("동기화 중 오류가 발생하면 실패 카운트가 포함된 리포트를 전송한다")
    void syncChangwonWelfareData_HandlesErrorAndSendsReport() {
        // Given
        WelfareApiResponse response = mock(WelfareApiResponse.class);
        WelfareApiItem item = mock(WelfareApiItem.class);
        
        when(item.getServiceId()).thenReturn("serv-err");
        when(response.getServiceList()).thenReturn(Collections.singletonList(item));
        when(response.getTotalCount()).thenReturn(1);
        
        when(welfareClient.fetchWelfareList(anyInt(), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(response);
        when(welfareServiceRepository.findById("serv-err")).thenReturn(Optional.empty());
        doThrow(new RuntimeException("DB Error")).when(welfareServiceRepository).save(any());

        // When
        WelfareSyncResultResponse result = welfareSyncService.syncChangwonWelfareData();

        // Then
        assertThat(result.totalProcessedCount()).isEqualTo(1);
        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
        verify(discordUtil).sendMessage(contains("일부 실패 발생"));
    }

    @Test
    @DisplayName("목록 응답이 null이면 처리 없이 정상 종료한다")
    void syncAllWelfareData_stopsWhenListResponseIsNull() {
        when(welfareClient.fetchWelfareList(anyInt(), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(null);

        WelfareSyncResultResponse result = welfareSyncService.syncAllWelfareData();

        assertThat(result.totalProcessedCount()).isZero();
        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isZero();
        verify(discordUtil).sendMessage(contains("정상 완료"));
    }

    @Test
    @DisplayName("목록 응답의 서비스 목록이 null이면 처리 없이 종료한다")
    void syncAllWelfareData_stopsWhenServiceListIsNull() {
        WelfareApiResponse response = mock(WelfareApiResponse.class);
        when(response.getServiceList()).thenReturn(null);
        when(welfareClient.fetchWelfareList(anyInt(), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(response);

        WelfareSyncResultResponse result = welfareSyncService.syncAllWelfareData();

        assertThat(result.totalProcessedCount()).isZero();
        verify(discordUtil).sendMessage(contains("정상 완료"));
    }

    @Test
    @DisplayName("목록 응답의 서비스 목록이 비어 있으면 처리 없이 종료한다")
    void syncAllWelfareData_stopsWhenServiceListIsEmpty() {
        WelfareApiResponse response = mock(WelfareApiResponse.class);
        when(response.getServiceList()).thenReturn(Collections.emptyList());
        when(welfareClient.fetchWelfareList(anyInt(), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(response);

        WelfareSyncResultResponse result = welfareSyncService.syncAllWelfareData();

        assertThat(result.totalProcessedCount()).isZero();
        verify(discordUtil).sendMessage(contains("정상 완료"));
    }

    @Test
    @DisplayName("목록에 null item이 있으면 항목 실패로 처리한다")
    void syncAllWelfareData_countsFailureWhenItemIsNull() {
        WelfareApiResponse response = mock(WelfareApiResponse.class);
        when(response.getServiceList()).thenReturn(Collections.singletonList(null));
        when(response.getTotalCount()).thenReturn(1);
        when(welfareClient.fetchWelfareList(anyInt(), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(response);

        WelfareSyncResultResponse result = welfareSyncService.syncAllWelfareData();

        assertThat(result.failureCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("목록 응답의 totalCount가 null이면 현재 페이지만 처리하고 종료한다")
    void syncAllWelfareData_stopsAfterCurrentPageWhenTotalCountIsNull() {
        WelfareApiResponse response = mock(WelfareApiResponse.class);
        WelfareApiItem item = mock(WelfareApiItem.class);

        when(item.getServiceId()).thenReturn("TOTAL_NULL");
        when(item.getServiceName()).thenReturn("복지");
        when(response.getServiceList()).thenReturn(Collections.singletonList(item));
        when(response.getTotalCount()).thenReturn(null);
        when(welfareClient.fetchWelfareList(anyInt(), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(response);
        when(welfareServiceRepository.findById("TOTAL_NULL")).thenReturn(Optional.empty());
        when(welfareClient.fetchWelfareDetail("TOTAL_NULL")).thenReturn(detailSampleJson);

        WelfareSyncResultResponse result = welfareSyncService.syncAllWelfareData();

        assertThat(result.totalProcessedCount()).isEqualTo(1);
        verify(welfareClient, times(1)).fetchWelfareList(anyInt(), anyInt(), nullable(String.class), nullable(String.class));
    }

    @Test
    @DisplayName("목록 응답이 여러 페이지면 다음 페이지까지 조회한다")
    void syncAllWelfareData_fetchesNextPageWhenTotalCountExceedsCurrentPage() {
        WelfareApiResponse firstResponse = mock(WelfareApiResponse.class);
        WelfareApiResponse secondResponse = mock(WelfareApiResponse.class);
        WelfareApiItem firstItem = mock(WelfareApiItem.class);
        WelfareApiItem secondItem = mock(WelfareApiItem.class);

        when(firstItem.getServiceId()).thenReturn("PAGE_1");
        when(firstItem.getServiceName()).thenReturn("1페이지 복지");
        when(secondItem.getServiceId()).thenReturn("PAGE_2");
        when(secondItem.getServiceName()).thenReturn("2페이지 복지");
        when(firstResponse.getServiceList()).thenReturn(Collections.singletonList(firstItem));
        when(firstResponse.getTotalCount()).thenReturn(101);
        when(secondResponse.getServiceList()).thenReturn(Collections.singletonList(secondItem));
        when(secondResponse.getTotalCount()).thenReturn(101);
        when(welfareClient.fetchWelfareList(eq(1), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(firstResponse);
        when(welfareClient.fetchWelfareList(eq(2), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(secondResponse);
        when(welfareServiceRepository.findById("PAGE_1")).thenReturn(Optional.empty());
        when(welfareServiceRepository.findById("PAGE_2")).thenReturn(Optional.empty());
        when(welfareClient.fetchWelfareDetail(anyString())).thenReturn(detailSampleJson);

        WelfareSyncResultResponse result = welfareSyncService.syncAllWelfareData();

        assertThat(result.totalProcessedCount()).isEqualTo(2);
        verify(welfareClient).fetchWelfareList(eq(2), anyInt(), nullable(String.class), nullable(String.class));
    }

    @Test
    @DisplayName("목록 API 호출 자체가 실패하면 치명 오류 리포트를 전송하고 현재 결과를 반환한다")
    void syncAllWelfareData_returnsCurrentResultWhenListFetchFails() {
        when(welfareClient.fetchWelfareList(anyInt(), anyInt(), nullable(String.class), nullable(String.class)))
                .thenThrow(new RuntimeException("network error"));

        WelfareSyncResultResponse result = welfareSyncService.syncAllWelfareData();

        assertThat(result.totalProcessedCount()).isZero();
        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isZero();
        verify(discordUtil).sendMessage(contains("치명적 오류 발생"));
    }

    @Test
    @DisplayName("복지 동기화 시 XML 상세 응답도 상세 필드와 목록성 원문 JSON으로 저장한다")
    void syncChangwonWelfareData_parsesXmlDetailResponse() {
        WelfareApiResponse response = mock(WelfareApiResponse.class);
        WelfareApiItem item = mock(WelfareApiItem.class);

        when(item.getServiceId()).thenReturn("WLF00006281");
        when(item.getServiceName()).thenReturn("방학기간 초등아동 자녀 일시 배달비 지원사업");
        when(item.getCityProvinceName()).thenReturn("전라남도");
        when(item.getDistrictName()).thenReturn("광양시");
        when(response.getServiceList()).thenReturn(Collections.singletonList(item));
        when(response.getTotalCount()).thenReturn(1);
        when(welfareClient.fetchWelfareList(anyInt(), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(response);
        when(welfareServiceRepository.findById("WLF00006281")).thenReturn(Optional.empty());
        when(welfareClient.fetchWelfareDetail("WLF00006281")).thenReturn(detailSampleXml);

        WelfareSyncResultResponse result = welfareSyncService.syncChangwonWelfareData();

        assertThat(result.successCount()).isEqualTo(1);
        ArgumentCaptor<WelfareServiceEntity> captor = ArgumentCaptor.forClass(WelfareServiceEntity.class);
        verify(welfareServiceRepository).save(captor.capture());
        WelfareServiceEntity saved = captor.getValue();
        assertThat(saved.getTargetDetailContent()).contains("초등아동");
        assertThat(saved.getBenefitContent()).isEqualTo("방학기간 배달비 지원");
        assertThat(saved.getApplicationMethodList()).isEqualTo("모바일앱 또는 인터넷 신청");
        assertThat(saved.getHomepageUrl()).isEqualTo("https://www.bokjiro.go.kr");
        assertThat(saved.getInquiryContactList()).contains("문의처");
        assertThat(saved.getHomepageList()).contains("홈페이지");
        assertThat(saved.getBasisLawList()).contains("근거법");
        assertThat(saved.getBasisFormList()).contains("신청서");
    }

    @Test
    @DisplayName("복지 동기화는 실제 XML 상세 응답처럼 홈페이지 목록이 없어도 나머지 상세 필드를 저장한다")
    void syncChangwonWelfareData_parsesRealXmlDetailResponseWithoutHomepageList() {
        WelfareApiResponse response = mock(WelfareApiResponse.class);
        WelfareApiItem item = mock(WelfareApiItem.class);

        when(item.getServiceId()).thenReturn("WLF00001516");
        when(item.getServiceName()).thenReturn("아빠 육아휴직 장려금 지원");
        when(item.getCityProvinceName()).thenReturn("인천광역시");
        when(item.getDistrictName()).thenReturn("계양구");
        when(response.getServiceList()).thenReturn(Collections.singletonList(item));
        when(response.getTotalCount()).thenReturn(1);
        when(welfareClient.fetchWelfareList(anyInt(), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(response);
        when(welfareServiceRepository.findById("WLF00001516")).thenReturn(Optional.empty());
        when(welfareClient.fetchWelfareDetail("WLF00001516")).thenReturn(detailRealSampleXml);

        WelfareSyncResultResponse result = welfareSyncService.syncChangwonWelfareData();

        assertThat(result.successCount()).isEqualTo(1);
        ArgumentCaptor<WelfareServiceEntity> captor = ArgumentCaptor.forClass(WelfareServiceEntity.class);
        verify(welfareServiceRepository).save(captor.capture());
        WelfareServiceEntity saved = captor.getValue();
        assertThat(saved.getTargetDetailContent()).contains("남성 육아휴직자");
        assertThat(saved.getSelectionCriteriaContent()).contains("남성 육아휴직자");
        assertThat(saved.getBenefitContent()).contains("매월 50만원");
        assertThat(saved.getApplicationMethodList()).contains("계양구청 여성보육과");
        assertThat(saved.getHomepageUrl()).isNull();
        assertThat(saved.getHomepageList()).isNull();
        assertThat(saved.getInquiryContactList()).contains("032-450-5984");
        assertThat(saved.getBasisLawList()).contains("아빠 육아휴직 장려금 지원 조례");
        assertThat(saved.getBasisFormList()).contains("아빠 육아휴직 장려금 지원 신청서");
    }

    @Test
    @DisplayName("전체 동기화에서 이번 목록에 없는 기존 활성 서비스는 종료 처리한다")
    void syncAllWelfareData_marksMissingActiveServiceAsDiscontinued() {
        WelfareApiResponse response = mock(WelfareApiResponse.class);
        WelfareApiItem currentItem = mock(WelfareApiItem.class);
        WelfareServiceEntity oldActiveService = WelfareServiceEntity.builder()
                .serviceId("OLD_SERVICE")
                .serviceName("이전 복지")
                .build();

        when(currentItem.getServiceId()).thenReturn("CURRENT_SERVICE");
        when(currentItem.getServiceName()).thenReturn("현재 복지");
        when(currentItem.getCityProvinceName()).thenReturn("서울특별시");
        when(currentItem.getDistrictName()).thenReturn("강남구");
        when(response.getServiceList()).thenReturn(Collections.singletonList(currentItem));
        when(response.getTotalCount()).thenReturn(1);
        when(welfareClient.fetchWelfareList(anyInt(), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(response);
        when(welfareServiceRepository.findById("CURRENT_SERVICE")).thenReturn(Optional.empty());
        when(welfareClient.fetchWelfareDetail("CURRENT_SERVICE")).thenReturn(detailSampleJson);
        when(welfareServiceRepository.findAllByActiveTrue()).thenReturn(List.of(oldActiveService));

        WelfareSyncResultResponse result = welfareSyncService.syncAllWelfareData();

        assertThat(result.discontinuedCount()).isEqualTo(1);
        assertThat(oldActiveService.getActive()).isFalse();
        assertThat(oldActiveService.getDiscontinuedAt()).isNotNull();
    }

    @Test
    @DisplayName("전체 동기화에서 이번 목록에 포함된 기존 활성 서비스는 종료 처리하지 않는다")
    void syncAllWelfareData_keepsCurrentActiveServiceActive() {
        WelfareApiResponse response = mock(WelfareApiResponse.class);
        WelfareApiItem currentItem = mock(WelfareApiItem.class);
        WelfareServiceEntity currentActiveService = WelfareServiceEntity.builder()
                .serviceId("CURRENT_ACTIVE")
                .serviceName("현재 복지")
                .build();

        when(currentItem.getServiceId()).thenReturn("CURRENT_ACTIVE");
        when(currentItem.getServiceName()).thenReturn("현재 복지");
        when(response.getServiceList()).thenReturn(Collections.singletonList(currentItem));
        when(response.getTotalCount()).thenReturn(1);
        when(welfareClient.fetchWelfareList(anyInt(), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(response);
        when(welfareServiceRepository.findById("CURRENT_ACTIVE")).thenReturn(Optional.of(currentActiveService));
        when(welfareClient.fetchWelfareDetail("CURRENT_ACTIVE")).thenReturn(detailSampleJson);
        when(welfareServiceRepository.findAllByActiveTrue()).thenReturn(List.of(currentActiveService));

        WelfareSyncResultResponse result = welfareSyncService.syncAllWelfareData();

        assertThat(result.discontinuedCount()).isZero();
        assertThat(currentActiveService.getActive()).isTrue();
        assertThat(currentActiveService.getDiscontinuedAt()).isNull();
    }

    @Test
    @DisplayName("기존 서비스의 lastModYmd가 동일하고 상세가 이미 동기화되어 있으면 상세 API 호출을 생략한다")
    void syncAllWelfareData_skipsDetailFetchWhenLastModifiedDateIsUnchanged() {
        WelfareApiResponse response = mock(WelfareApiResponse.class);
        WelfareApiItem item = mock(WelfareApiItem.class);
        WelfareServiceEntity existingService = WelfareServiceEntity.builder()
                .serviceId("UNCHANGED_SERVICE")
                .serviceName("기존 복지")
                .lastModifiedDate("20260516")
                .build();
        existingService.updateDetailInfo("대상", "혜택", "기준", "신청", null, LocalDateTime.now().minusDays(1));

        when(item.getServiceId()).thenReturn("UNCHANGED_SERVICE");
        when(item.getServiceName()).thenReturn("기존 복지");
        when(item.getCityProvinceName()).thenReturn("서울특별시");
        when(item.getDistrictName()).thenReturn("강남구");
        when(item.getLastModifiedDate()).thenReturn("20260516");
        when(response.getServiceList()).thenReturn(Collections.singletonList(item));
        when(response.getTotalCount()).thenReturn(1);
        when(welfareClient.fetchWelfareList(anyInt(), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(response);
        when(welfareServiceRepository.findById("UNCHANGED_SERVICE")).thenReturn(Optional.of(existingService));

        WelfareSyncResultResponse result = welfareSyncService.syncAllWelfareData();

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.detailFailureCount()).isZero();
        verify(welfareClient, never()).fetchWelfareDetail("UNCHANGED_SERVICE");
        assertThat(existingService.getDetailSyncFailed()).isFalse();
    }

    @Test
    @DisplayName("기존 서비스의 lastModYmd가 변경되면 상세 API를 다시 호출한다")
    void syncAllWelfareData_fetchesDetailWhenLastModifiedDateChanged() {
        WelfareApiResponse response = mock(WelfareApiResponse.class);
        WelfareApiItem item = mock(WelfareApiItem.class);
        WelfareServiceEntity existingService = WelfareServiceEntity.builder()
                .serviceId("CHANGED_SERVICE")
                .serviceName("기존 복지")
                .lastModifiedDate("20260515")
                .build();
        existingService.updateDetailInfo("대상", "혜택", "기준", "신청", null, LocalDateTime.now().minusDays(1));

        when(item.getServiceId()).thenReturn("CHANGED_SERVICE");
        when(item.getServiceName()).thenReturn("기존 복지");
        when(item.getCityProvinceName()).thenReturn("서울특별시");
        when(item.getDistrictName()).thenReturn("강남구");
        when(item.getLastModifiedDate()).thenReturn("20260516");
        when(response.getServiceList()).thenReturn(Collections.singletonList(item));
        when(response.getTotalCount()).thenReturn(1);
        when(welfareClient.fetchWelfareList(anyInt(), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(response);
        when(welfareServiceRepository.findById("CHANGED_SERVICE")).thenReturn(Optional.of(existingService));
        when(welfareClient.fetchWelfareDetail("CHANGED_SERVICE")).thenReturn(detailRealSampleXml);

        WelfareSyncResultResponse result = welfareSyncService.syncAllWelfareData();

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.detailFailureCount()).isZero();
        verify(welfareClient).fetchWelfareDetail("CHANGED_SERVICE");
        assertThat(existingService.getLastModifiedDate()).isEqualTo("20260516");
        assertThat(existingService.getBenefitContent()).contains("매월 50만원");
    }

    @Test
    @DisplayName("기존 서비스의 상세 동기화가 실패 상태면 수정일이 같아도 상세 API를 다시 호출한다")
    void syncAllWelfareData_fetchesDetailWhenPreviousDetailSyncFailed() {
        WelfareApiResponse response = mock(WelfareApiResponse.class);
        WelfareApiItem item = mock(WelfareApiItem.class);
        WelfareServiceEntity existingService = WelfareServiceEntity.builder()
                .serviceId("FAILED_BEFORE")
                .serviceName("기존 복지")
                .lastModifiedDate("20260516")
                .build();
        existingService.updateDetailInfo("대상", "혜택", "기준", "신청", null, LocalDateTime.now().minusDays(1));
        existingService.markDetailSyncFailed("previous error");

        when(item.getServiceId()).thenReturn("FAILED_BEFORE");
        when(item.getServiceName()).thenReturn("기존 복지");
        when(item.getLastModifiedDate()).thenReturn("20260516");
        when(response.getServiceList()).thenReturn(Collections.singletonList(item));
        when(response.getTotalCount()).thenReturn(1);
        when(welfareClient.fetchWelfareList(anyInt(), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(response);
        when(welfareServiceRepository.findById("FAILED_BEFORE")).thenReturn(Optional.of(existingService));
        when(welfareClient.fetchWelfareDetail("FAILED_BEFORE")).thenReturn(detailRealSampleXml);

        welfareSyncService.syncAllWelfareData();

        verify(welfareClient).fetchWelfareDetail("FAILED_BEFORE");
        assertThat(existingService.getDetailSyncFailed()).isFalse();
    }

    @Test
    @DisplayName("기존 서비스의 lastModYmd가 없고 상세가 이미 동기화되어 있으면 상세 API 호출을 생략한다")
    void syncAllWelfareData_skipsDetailFetchWhenLastModifiedDateIsBlank() {
        WelfareApiResponse response = mock(WelfareApiResponse.class);
        WelfareApiItem item = mock(WelfareApiItem.class);
        WelfareServiceEntity existingService = WelfareServiceEntity.builder()
                .serviceId("NO_LAST_MOD")
                .serviceName("기존 복지")
                .lastModifiedDate(null)
                .build();
        existingService.updateDetailInfo("대상", "혜택", "기준", "신청", null, LocalDateTime.now().minusDays(1));

        when(item.getServiceId()).thenReturn("NO_LAST_MOD");
        when(item.getServiceName()).thenReturn("기존 복지");
        when(item.getLastModifiedDate()).thenReturn(null);
        when(response.getServiceList()).thenReturn(Collections.singletonList(item));
        when(response.getTotalCount()).thenReturn(1);
        when(welfareClient.fetchWelfareList(anyInt(), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(response);
        when(welfareServiceRepository.findById("NO_LAST_MOD")).thenReturn(Optional.of(existingService));

        welfareSyncService.syncAllWelfareData();

        verify(welfareClient, never()).fetchWelfareDetail("NO_LAST_MOD");
    }

    @Test
    @DisplayName("기존 서비스의 상세 동기화 이력이 없으면 상세 API를 호출한다")
    void syncAllWelfareData_fetchesDetailWhenDetailNeverSynced() {
        WelfareApiResponse response = mock(WelfareApiResponse.class);
        WelfareApiItem item = mock(WelfareApiItem.class);
        WelfareServiceEntity existingService = WelfareServiceEntity.builder()
                .serviceId("NEVER_SYNCED")
                .serviceName("기존 복지")
                .lastModifiedDate("20260516")
                .build();

        when(item.getServiceId()).thenReturn("NEVER_SYNCED");
        when(item.getServiceName()).thenReturn("기존 복지");
        when(item.getLastModifiedDate()).thenReturn("20260516");
        when(response.getServiceList()).thenReturn(Collections.singletonList(item));
        when(response.getTotalCount()).thenReturn(1);
        when(welfareClient.fetchWelfareList(anyInt(), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(response);
        when(welfareServiceRepository.findById("NEVER_SYNCED")).thenReturn(Optional.of(existingService));
        when(welfareClient.fetchWelfareDetail("NEVER_SYNCED")).thenReturn(detailRealSampleXml);

        welfareSyncService.syncAllWelfareData();

        verify(welfareClient).fetchWelfareDetail("NEVER_SYNCED");
        assertThat(existingService.getDetailSyncedAt()).isNotNull();
    }

    @Test
    @DisplayName("부분 동기화에서는 종료 처리를 생략한다")
    void syncWelfareData_skipsDiscontinuedMarkingWhenDisabled() {
        when(welfareClient.fetchWelfareList(anyInt(), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(null);

        WelfareSyncResultResponse result = welfareSyncService.syncWelfareData("서울특별시", "강남구", false);

        assertThat(result.discontinuedCount()).isZero();
        verify(welfareServiceRepository, never()).findAllByActiveTrue();
    }

    @Test
    @DisplayName("상세 응답 파싱이 실패하면 목록은 저장하고 상세 실패 상태를 기록한다")
    void syncAllWelfareData_marksDetailFailureWhenDetailParsingFails() {
        WelfareApiResponse response = mock(WelfareApiResponse.class);
        WelfareApiItem item = mock(WelfareApiItem.class);

        when(item.getServiceId()).thenReturn("BROKEN_DETAIL");
        when(item.getServiceName()).thenReturn("상세 오류 복지");
        when(response.getServiceList()).thenReturn(Collections.singletonList(item));
        when(response.getTotalCount()).thenReturn(1);
        when(welfareClient.fetchWelfareList(anyInt(), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(response);
        when(welfareServiceRepository.findById("BROKEN_DETAIL")).thenReturn(Optional.empty());
        when(welfareClient.fetchWelfareDetail("BROKEN_DETAIL")).thenReturn("{broken-json");

        WelfareSyncResultResponse result = welfareSyncService.syncAllWelfareData();

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.detailFailureCount()).isEqualTo(1);
        ArgumentCaptor<WelfareServiceEntity> captor = ArgumentCaptor.forClass(WelfareServiceEntity.class);
        verify(welfareServiceRepository).save(captor.capture());
        assertThat(captor.getValue().getDetailSyncFailed()).isTrue();
        assertThat(captor.getValue().getDetailSyncFailureReason()).isNotBlank();
    }

    @Test
    @DisplayName("상세 응답이 null이면 상세 실패 상태로 저장한다")
    void syncAllWelfareData_marksDetailFailureWhenDetailResponseIsNull() {
        WelfareApiResponse response = mock(WelfareApiResponse.class);
        WelfareApiItem item = mock(WelfareApiItem.class);

        when(item.getServiceId()).thenReturn("NULL_DETAIL");
        when(item.getServiceName()).thenReturn("상세 없음 복지");
        when(response.getServiceList()).thenReturn(Collections.singletonList(item));
        when(response.getTotalCount()).thenReturn(1);
        when(welfareClient.fetchWelfareList(anyInt(), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(response);
        when(welfareServiceRepository.findById("NULL_DETAIL")).thenReturn(Optional.empty());
        when(welfareClient.fetchWelfareDetail("NULL_DETAIL")).thenReturn(null);

        WelfareSyncResultResponse result = welfareSyncService.syncAllWelfareData();

        assertThat(result.detailFailureCount()).isEqualTo(1);
        ArgumentCaptor<WelfareServiceEntity> captor = ArgumentCaptor.forClass(WelfareServiceEntity.class);
        verify(welfareServiceRepository).save(captor.capture());
        assertThat(captor.getValue().getDetailSyncFailed()).isTrue();
        assertThat(captor.getValue().getDetailSyncFailureReason()).isEqualTo("empty detail response");
    }

    @Test
    @DisplayName("상세 응답이 blank이면 상세 실패 상태로 저장한다")
    void syncAllWelfareData_marksDetailFailureWhenDetailResponseIsBlank() {
        WelfareApiResponse response = mock(WelfareApiResponse.class);
        WelfareApiItem item = mock(WelfareApiItem.class);

        when(item.getServiceId()).thenReturn("BLANK_DETAIL");
        when(item.getServiceName()).thenReturn("상세 공백 복지");
        when(response.getServiceList()).thenReturn(Collections.singletonList(item));
        when(response.getTotalCount()).thenReturn(1);
        when(welfareClient.fetchWelfareList(anyInt(), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(response);
        when(welfareServiceRepository.findById("BLANK_DETAIL")).thenReturn(Optional.empty());
        when(welfareClient.fetchWelfareDetail("BLANK_DETAIL")).thenReturn("   ");

        WelfareSyncResultResponse result = welfareSyncService.syncAllWelfareData();

        assertThat(result.detailFailureCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("상세 동기화 실패 재시도는 실패 상태 서비스를 다시 상세 동기화한다")
    void retryFailedDetailSync_updatesFailedDetailService() {
        WelfareServiceEntity failedService = WelfareServiceEntity.builder()
                .serviceId("FAILED_DETAIL")
                .serviceName("상세 실패 복지")
                .build();
        failedService.markDetailSyncFailed("timeout");

        when(welfareServiceRepository.findAllByActiveTrueAndDetailSyncFailedTrue()).thenReturn(List.of(failedService));
        when(welfareClient.fetchWelfareDetail("FAILED_DETAIL")).thenReturn(detailRealSampleXml);
        when(welfareServiceRepository.findById("FAILED_DETAIL")).thenReturn(Optional.of(failedService));

        WelfareSyncResultResponse result = welfareSyncService.retryFailedDetailSync();

        assertThat(result.totalProcessedCount()).isEqualTo(1);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isZero();
        assertThat(result.detailFailureCount()).isZero();
        assertThat(failedService.getDetailSyncFailed()).isFalse();
        assertThat(failedService.getDetailSyncFailureReason()).isNull();
        assertThat(failedService.getBenefitContent()).contains("매월 50만원");
        verify(welfareServiceRepository).save(failedService);
    }

    @Test
    @DisplayName("상세 동기화 실패 재시도 중 다시 실패하면 실패 상태를 유지한다")
    void retryFailedDetailSync_keepsFailureWhenRetryFailsAgain() {
        WelfareServiceEntity failedService = WelfareServiceEntity.builder()
                .serviceId("FAILED_AGAIN")
                .serviceName("상세 실패 복지")
                .build();
        failedService.markDetailSyncFailed("timeout");

        when(welfareServiceRepository.findAllByActiveTrueAndDetailSyncFailedTrue()).thenReturn(List.of(failedService));
        when(welfareClient.fetchWelfareDetail("FAILED_AGAIN")).thenThrow(new RuntimeException("still failing"));
        when(welfareServiceRepository.findById("FAILED_AGAIN")).thenReturn(Optional.of(failedService));

        WelfareSyncResultResponse result = welfareSyncService.retryFailedDetailSync();

        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.detailFailureCount()).isEqualTo(1);
        assertThat(failedService.getDetailSyncFailed()).isTrue();
        assertThat(failedService.getDetailSyncFailureReason()).contains("still failing");
    }

    @Test
    @DisplayName("상세 파서의 null, object, array, 누락 분기를 검증한다")
    void detailSyncParser_coverRecursiveBranches() throws Exception {
        JsonNode root = objectMapper.readTree("""
                {
                  "wrapper": {
                    "items": [
                      {"unused": "x"},
                      {"target": "found", "raw": {"value": "raw-found"}},
                      {"nested": {"target": "nested-found"}}
                    ],
                    "nullable": null,
                    "homepageUrl": "https://direct.example.com"
                  }
                }
                """);
        JsonNode nestedRoot = objectMapper.readTree("{\"outer\":{\"target\":\"nested-direct\"}}");
        JsonNode missingNestedRoot = objectMapper.readTree("{\"outer\":{\"noTarget\":\"x\"}}");

        String nullText = detailSyncParser.findText((JsonNode) null, "target");
        String nullNodeText = detailSyncParser.findText(NullNode.getInstance(), "target");
        String nestedText = detailSyncParser.findText(root, "target");
        String objectValueText = detailSyncParser.findText(nestedRoot, "outer", "target");
        String missingObjectValueText = detailSyncParser.findText(missingNestedRoot, "outer", "target");
        String missingText = detailSyncParser.findText(root, "missing");
        String nullableText = detailSyncParser.findText(root, "nullable");
        JsonNode nullFindNode = detailSyncParser.findNode((JsonNode) null, "missing");
        JsonNode nullableFindNode = detailSyncParser.findNode(root, "nullable");
        JsonNode missingNode = detailSyncParser.findNode(root, "missing");
        JsonNode nestedNode = detailSyncParser.findNode(root, "nested");
        String nullRaw = detailSyncParser.findRawJson((JsonNode) null, "raw");
        String nullableRaw = detailSyncParser.findRawJson(root, "nullable");
        String nestedRaw = detailSyncParser.findRawJson(root, "raw");
        String missingRaw = detailSyncParser.findRawJson(root, "missing");
        String directHomepage = detailSyncParser.findHomepageUrl(root);

        assertThat(nullText).isNull();
        assertThat(nullNodeText).isNull();
        assertThat(nestedText).isEqualTo("found");
        assertThat(objectValueText).isEqualTo("nested-direct");
        assertThat(missingObjectValueText).isNull();
        assertThat(missingText).isNull();
        assertThat(nullableText).isNull();
        assertThat(nullFindNode).isNull();
        assertThat(nullableFindNode).isNull();
        assertThat(missingNode).isNull();
        assertThat(nestedNode).isNotNull();
        assertThat(nullRaw).isNull();
        assertThat(nullableRaw).isNull();
        assertThat(nestedRaw).contains("raw-found");
        assertThat(missingRaw).isNull();
        assertThat(directHomepage).isEqualTo("https://direct.example.com");
    }
}
