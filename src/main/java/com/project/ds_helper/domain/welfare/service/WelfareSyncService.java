package com.project.ds_helper.domain.welfare.service;

import com.project.ds_helper.common.util.DiscordWebhookUtil;
import com.project.ds_helper.domain.welfare.client.PublicDataWelfareClient;
import com.project.ds_helper.domain.welfare.dto.external.WelfareApiItem;
import com.project.ds_helper.domain.welfare.dto.external.WelfareApiResponse;
import com.project.ds_helper.domain.welfare.entity.WelfareServiceEntity;
import com.project.ds_helper.domain.welfare.repository.WelfareServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 복지 데이터 동기화 서비스
 * 공공데이터 API로부터 주기적으로 데이터를 가져와 DB를 최신화합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WelfareSyncService {

    private final PublicDataWelfareClient welfareClient;
    private final WelfareServiceRepository welfareServiceRepository;
    private final DiscordWebhookUtil discordUtil;

    /**
     * 매일 새벽 3시에 창원시 복지 데이터를 동기화합니다.
     * 동기화 완료 후 결과를 Discord로 알림 발송합니다.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void syncChangwonWelfareData() {
        log.info("=== 창원시 복지 데이터 동기화 프로세스 시작 ===");
        
        int totalProcessedCount = 0;
        int successCount = 0;
        int failureCount = 0;
        int pageNumber = 1;
        int numberOfRows = 100;
        
        while (true) {
            try {
                WelfareApiResponse response = welfareClient.fetchWelfareList(pageNumber, numberOfRows, "경상남도", "창원시");
                
                if (response == null || response.getServiceList() == null || response.getServiceList().isEmpty()) {
                    break;
                }

                List<WelfareApiItem> items = response.getServiceList();
                for (WelfareApiItem item : items) {
                    totalProcessedCount++;
                    try {
                        upsertWelfareService(item);
                        successCount++;
                    } catch (Exception e) {
                        failureCount++;
                        log.error("복지 서비스 저장 실패 - serviceId: {}, 사유: {}", item.getServiceId(), e.getMessage());
                    }
                }

                if (pageNumber * numberOfRows >= response.getTotalCount()) break;
                pageNumber++;
                
            } catch (Exception e) {
                log.error("동기화 루프 중 오류 발생: {}", e.getMessage());
                discordUtil.sendMessage("🚨 **복지 데이터 동기화 중 치명적 에러 발생**\n사유: " + e.getMessage());
                return;
            }
        }
        
        // Discord 리포트 전송
        sendSyncReport(totalProcessedCount, successCount, failureCount);
        log.info("=== 창원시 복지 데이터 동기화 프로세스 종료 (총: {}, 성공: {}, 실패: {}) ===", totalProcessedCount, successCount, failureCount);
    }

    /**
     * 동기화 결과를 Discord로 전송합니다.
     */
    private void sendSyncReport(int total, int success, int failure) {
        String status = (failure == 0) ? "✅ 정상 완료" : "⚠️ 일부 실패 발생";
        String message = String.format(
            "📢 **복지 데이터 동기화 결과 보고**\n" +
            "- 상태: %s\n" +
            "- 총 처리: %d건\n" +
            "- 성공: %d건\n" +
            "- 실패: %d건",
            status, total, success, failure
        );
        discordUtil.sendMessage(message);
    }

    /**
     * 개별 복지 서비스 데이터를 DB에 저장하거나 업데이트합니다. (Upsert)
     */
    private void upsertWelfareService(WelfareApiItem item) {
        Objects.requireNonNull(item.getServiceId(), "서비스 ID는 필수입니다.");

        final WelfareServiceEntity entity = WelfareServiceEntity.builder()
                .serviceId(item.getServiceId())
                .serviceName(item.getServiceName())
                .serviceSummary(item.getServiceSummary())
                .jurisdictionMinistryName(item.getJurisdictionMinistryName())
                .targetAudienceArray(item.getTargetIndividualArray())
                .lifeCycleArray(item.getLifeCycleArray())
                .interestThemeArray(item.getInterestThemeArray())
                .serviceProvisionName(item.getServiceProvisionName())
                .representativeContact(item.getRepresentativeContact())
                .serviceDetailUrl(item.getServiceDetailUrl())
                .build();

        welfareServiceRepository.save(Objects.requireNonNull(entity));
    }
}
