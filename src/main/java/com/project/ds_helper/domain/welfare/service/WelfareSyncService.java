package com.project.ds_helper.domain.welfare.service;

import com.project.ds_helper.common.util.DiscordWebhookUtil;
import com.project.ds_helper.domain.welfare.client.PublicDataWelfareClient;
import com.project.ds_helper.domain.welfare.dto.external.WelfareApiItem;
import com.project.ds_helper.domain.welfare.dto.external.WelfareApiResponse;
import com.project.ds_helper.domain.welfare.dto.response.WelfareSyncResultResponse;
import com.project.ds_helper.domain.welfare.entity.WelfareServiceEntity;
import com.project.ds_helper.domain.welfare.repository.WelfareServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 공공데이터포털 복지 서비스 목록/상세 데이터를 DB에 동기화한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WelfareSyncService {

    private static final int DEFAULT_PAGE_SIZE = 100;

    private final PublicDataWelfareClient welfareClient;
    private final WelfareServiceRepository welfareServiceRepository;
    private final DiscordWebhookUtil discordUtil;
    private final TransactionTemplate transactionTemplate;
    private final WelfareDetailSyncParser detailSyncParser = new WelfareDetailSyncParser();

    /**
     * 매일 새벽 3시에 전체 복지 데이터를 동기화한다.
     * 목록 저장 후 상세 API까지 호출해 상세 조회 응답에 필요한 필드를 함께 채운다.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public WelfareSyncResultResponse syncAllWelfareData() {
        return syncWelfareData(null, null, true);
    }

    public WelfareSyncResultResponse syncChangwonWelfareData() {
        return syncAllWelfareData();
    }

    public WelfareSyncResultResponse syncWelfareData(String cityProvinceName, String districtName, boolean markDiscontinued) {
        log.info("WelfareSyncService.syncWelfareData started. cityProvinceName={}, districtName={}, markDiscontinued={}",
                cityProvinceName, districtName, markDiscontinued);

        LocalDateTime syncStartedAt = LocalDateTime.now();
        Set<String> currentServiceIds = new HashSet<>();
        int totalProcessedCount = 0;
        int successCount = 0;
        int failureCount = 0;
        int detailFailureCount = 0;
        int pageNumber = 1;
        int numberOfRows = DEFAULT_PAGE_SIZE;

        while (true) {
            try {
                WelfareApiResponse response = welfareClient.fetchWelfareList(
                        pageNumber,
                        numberOfRows,
                        cityProvinceName,
                        districtName
                );

                if (response == null || response.getServiceList() == null || response.getServiceList().isEmpty()) {
                    log.debug("WelfareSyncService.syncWelfareData stopped. empty response. pageNumber={}", pageNumber);
                    break;
                }

                List<WelfareApiItem> items = response.getServiceList();
                for (WelfareApiItem item : items) {
                    totalProcessedCount++;
                    try {
                        String serviceId = Objects.requireNonNull(item.getServiceId(), "serviceId is required");
                        currentServiceIds.add(serviceId);
                        DetailSyncPayload detailSyncPayload = upsertWelfareService(item, cityProvinceName, districtName, syncStartedAt);
                        if (!detailSyncPayload.success() && !detailSyncPayload.detailSkipped()) {
                            detailFailureCount++;
                        }
                        successCount++;
                    } catch (Exception e) {
                        failureCount++;
                        log.error("WelfareSyncService.syncWelfareData item failed. serviceId={}, reason={}",
                                item == null ? null : item.getServiceId(), e.getMessage(), e);
                    }
                }

                if (response.getTotalCount() == null || pageNumber * numberOfRows >= response.getTotalCount()) {
                    break;
                }
                pageNumber++;
            } catch (Exception e) {
                log.error("WelfareSyncService.syncWelfareData loop failed. pageNumber={}, reason={}",
                        pageNumber, e.getMessage(), e);
                discordUtil.sendMessage("복지 데이터 동기화 중 치명적 오류 발생\n사유: " + e.getMessage());
                return new WelfareSyncResultResponse(totalProcessedCount, successCount, failureCount, 0, detailFailureCount);
            }
        }

        int discontinuedCount = markDiscontinued ? markDiscontinuedServices(currentServiceIds, syncStartedAt) : 0;
        sendSyncReport(totalProcessedCount, successCount, failureCount, discontinuedCount, detailFailureCount);
        log.info("WelfareSyncService.syncWelfareData completed. total={}, success={}, failure={}, discontinued={}, detailFailure={}",
                totalProcessedCount, successCount, failureCount, discontinuedCount, detailFailureCount);
        return new WelfareSyncResultResponse(totalProcessedCount, successCount, failureCount, discontinuedCount, detailFailureCount);
    }

    /**
     * 동기화 결과를 Discord로 전송한다.
     */
    private void sendSyncReport(int total, int success, int failure, int discontinued, int detailFailure) {
        String status = (failure == 0) ? "정상 완료" : "일부 실패 발생";
        String message = String.format(
                "복지 데이터 동기화 결과 보고\n" +
                        "- 상태: %s\n" +
                        "- 총 처리: %d건\n" +
                        "- 성공: %d건\n" +
                        "- 실패: %d건\n" +
                        "- 종료 처리: %d건\n" +
                        "- 상세 실패: %d건",
                status, total, success, failure, discontinued, detailFailure
        );
        discordUtil.sendMessage(message);
    }

    /**
     * 목록 데이터는 기존 상세 데이터를 보존하면서 갱신하고, 상세 API 응답이 있으면 상세 필드도 함께 갱신한다.
     */
    private DetailSyncPayload upsertWelfareService(WelfareApiItem item, String cityProvinceName, String districtName, LocalDateTime syncStartedAt) {
        Objects.requireNonNull(item, "welfare item is required");
        Objects.requireNonNull(item.getServiceId(), "serviceId is required");

        Optional<WelfareServiceEntity> existingEntity = welfareServiceRepository.findById(item.getServiceId());

        // 외부 API 호출은 신규/수정/실패/미동기화 대상에만 수행해 전체 상세 재조회 비용을 줄인다.
        DetailSyncPayload detailSyncPayload = shouldFetchDetail(item, existingEntity)
                ? fetchDetailSyncPayload(item.getServiceId(), syncStartedAt)
                : DetailSyncPayload.skipped();

        transactionTemplate.executeWithoutResult(status -> {
            log.debug("WelfareSyncService.upsertWelfareService transaction started. serviceId={}", item.getServiceId());

            WelfareServiceEntity entity = existingEntity
                    .orElseGet(() -> WelfareServiceEntity.builder()
                            .serviceId(item.getServiceId())
                            .serviceName(item.getServiceName())
                            .build());

            String resolvedCityProvinceName = StringUtils.hasText(item.getCityProvinceName()) ? item.getCityProvinceName() : cityProvinceName;
            String resolvedDistrictName = StringUtils.hasText(item.getDistrictName()) ? item.getDistrictName() : districtName;

            // 1. 목록 API 기준 필드를 갱신한다. 기존 상세 필드는 이 단계에서 보존된다.
            entity.updateListInfo(
                    item.getServiceName(),
                    resolvedCityProvinceName,
                    resolvedDistrictName,
                    item.getServiceSummary(),
                    item.getJurisdictionMinistryName(),
                    item.getTargetIndividualArray(),
                    item.getLifeCycleArray(),
                    item.getInterestThemeArray(),
                    item.getServiceProvisionName(),
                    item.getRepresentativeContact(),
                    item.getServiceDetailUrl(),
                    item.getLastModifiedDate()
            );
            entity.markSeen(syncStartedAt);

            // 2. 상세 API 호출 실패는 목록 저장 실패로 전파하지 않고, 실패 상태만 Entity에 기록한다.
            applyDetailSyncPayload(entity, detailSyncPayload);

            welfareServiceRepository.save(entity);
            log.debug("WelfareSyncService.upsertWelfareService transaction completed. serviceId={}", item.getServiceId());
        });
        return detailSyncPayload;
    }

    private boolean shouldFetchDetail(WelfareApiItem item, Optional<WelfareServiceEntity> existingEntity) {
        if (existingEntity.isEmpty()) {
            log.debug("WelfareSyncService.shouldFetchDetail true. reason=new serviceId={}", item.getServiceId());
            return true;
        }

        WelfareServiceEntity entity = existingEntity.get();
        if (Boolean.TRUE.equals(entity.getDetailSyncFailed())) {
            log.debug("WelfareSyncService.shouldFetchDetail true. reason=previous detail failure serviceId={}", item.getServiceId());
            return true;
        }
        if (entity.getDetailSyncedAt() == null) {
            log.debug("WelfareSyncService.shouldFetchDetail true. reason=detail never synced serviceId={}", item.getServiceId());
            return true;
        }
        if (StringUtils.hasText(item.getLastModifiedDate())
                && !Objects.equals(item.getLastModifiedDate(), entity.getLastModifiedDate())) {
            log.debug("WelfareSyncService.shouldFetchDetail true. reason=lastModYmd changed serviceId={}, old={}, new={}",
                    item.getServiceId(), entity.getLastModifiedDate(), item.getLastModifiedDate());
            return true;
        }

        log.debug("WelfareSyncService.shouldFetchDetail false. serviceId={}, lastModYmd={}",
                item.getServiceId(), item.getLastModifiedDate());
        return false;
    }

    private DetailSyncPayload fetchDetailSyncPayload(String serviceId, LocalDateTime syncStartedAt) {
        try {
            log.debug("WelfareSyncService.fetchDetailSyncPayload started. serviceId={}", serviceId);
            String detailJson = welfareClient.fetchWelfareDetail(serviceId);
            if (detailJson == null || detailJson.isBlank()) {
                log.debug("WelfareSyncService.fetchDetailSyncPayload skipped. empty detail response. serviceId={}",
                        serviceId);
                return DetailSyncPayload.failed("empty detail response");
            }

            var root = detailSyncParser.readJsonOrXmlTree(detailJson);
            log.debug("WelfareSyncService.fetchDetailSyncPayload completed. serviceId={}", serviceId);
            return detailSyncParser.buildDetailSyncPayload(root, syncStartedAt);
        } catch (Exception e) {
            log.warn("WelfareSyncService.fetchDetailSyncPayload failed. serviceId={}, reason={}",
                    serviceId, e.getMessage());
            return DetailSyncPayload.failed(e.getMessage());
        }
    }

    private void applyDetailSyncPayload(WelfareServiceEntity entity, DetailSyncPayload payload) {
        if (!payload.success()) {
            if (payload.detailSkipped()) {
                return;
            }
            entity.markDetailSyncFailed(payload.failureReason());
            return;
        }

        entity.updateDetailInfo(
                payload.targetDetailContent(),
                payload.benefitContent(),
                payload.selectionCriteriaContent(),
                payload.applicationMethodList(),
                payload.homepageUrl(),
                payload.detailSyncedAt()
        );
        entity.updateDetailRelationInfo(
                payload.inquiryContactList(),
                payload.homepageList(),
                payload.basisLawList(),
                payload.basisFormList()
        );
    }

    public WelfareSyncResultResponse retryFailedDetailSync() {
        LocalDateTime retryStartedAt = LocalDateTime.now();
        List<WelfareServiceEntity> failedServices = welfareServiceRepository.findAllByActiveTrueAndDetailSyncFailedTrue();
        int successCount = 0;
        int failureCount = 0;

        for (WelfareServiceEntity failedService : failedServices) {
            DetailSyncPayload payload = fetchDetailSyncPayload(failedService.getServiceId(), retryStartedAt);
            if (payload.success()) {
                successCount++;
            } else {
                failureCount++;
            }

            transactionTemplate.executeWithoutResult(status -> {
                WelfareServiceEntity entity = welfareServiceRepository.findById(failedService.getServiceId()).orElseThrow();
                applyDetailSyncPayload(entity, payload);
                welfareServiceRepository.save(entity);
            });
        }

        log.info("WelfareSyncService.retryFailedDetailSync completed. total={}, success={}, failure={}",
                failedServices.size(), successCount, failureCount);
        return new WelfareSyncResultResponse(failedServices.size(), successCount, failureCount, 0, failureCount);
    }

    public Optional<WelfareServiceEntity> fetchAndSaveWelfareDetailByServiceId(String serviceId) {
        log.info("WelfareSyncService.fetchAndSaveWelfareDetailByServiceId started. serviceId={}", serviceId);

        LocalDateTime syncedAt = LocalDateTime.now();
        try {
            String detailBody = welfareClient.fetchWelfareDetail(serviceId);
            if (detailBody == null || detailBody.isBlank()) {
                log.warn("WelfareSyncService.fetchAndSaveWelfareDetailByServiceId empty response. serviceId={}", serviceId);
                return Optional.empty();
            }

            var root = detailSyncParser.readJsonOrXmlTree(detailBody);
            DetailSyncPayload payload = detailSyncParser.buildDetailSyncPayload(root, syncedAt);
            String resolvedServiceId = detailSyncParser.findText(root, "servId", "serviceId");
            String serviceName = detailSyncParser.findText(root, "servNm", "serviceName");
            if (!StringUtils.hasText(resolvedServiceId) || !StringUtils.hasText(serviceName)) {
                log.warn("WelfareSyncService.fetchAndSaveWelfareDetailByServiceId invalid response. requestedServiceId={}, resolvedServiceId={}, serviceName={}",
                        serviceId, resolvedServiceId, serviceName);
                return Optional.empty();
            }

            WelfareServiceEntity savedEntity = transactionTemplate.execute(status -> {
                WelfareServiceEntity entity = welfareServiceRepository.findById(resolvedServiceId)
                        .orElseGet(() -> WelfareServiceEntity.builder()
                                .serviceId(resolvedServiceId)
                                .serviceName(serviceName)
                                .build());

                // 1. 상세 API 응답에 포함된 목록성 필드를 함께 채워 DB miss fallback 이후 추천/상세 조회 모두 동일한 Entity를 사용하게 한다.
                entity.updateListInfo(
                        serviceName,
                        detailSyncParser.findText(root, "ctpvNm", "cityProvinceName"),
                        detailSyncParser.findText(root, "sggNm", "districtName"),
                        detailSyncParser.findText(root, "servDgst", "serviceSummary"),
                        detailSyncParser.findText(root, "bizChrDeptNm", "jurMnstNm", "jurisdictionMinistryName"),
                        detailSyncParser.findText(root, "trgterIndvdlNmArray", "trgterIndvdlArray", "targetAudienceArray"),
                        detailSyncParser.findText(root, "lifeNmArray", "lifeArray", "lifeCycleArray"),
                        detailSyncParser.findText(root, "intrsThemaNmArray", "intrsThemaArray", "interestThemeArray"),
                        detailSyncParser.findText(root, "srvPvsnNm", "serviceProvisionName"),
                        detailSyncParser.findText(root, "inqNum", "rprsCtadr", "representativeContact"),
                        detailSyncParser.findText(root, "servDtlLink", "servDetlUrl", "serviceDetailUrl"),
                        detailSyncParser.findText(root, "lastModYmd", "lastModifiedDate")
                );
                entity.markSeen(syncedAt);

                // 2. 기존 상세 동기화와 같은 payload 적용 로직을 사용해 필드 매핑 차이를 만들지 않는다.
                applyDetailSyncPayload(entity, payload);
                return welfareServiceRepository.save(entity);
            });

            log.info("WelfareSyncService.fetchAndSaveWelfareDetailByServiceId completed. serviceId={}", resolvedServiceId);
            return Optional.ofNullable(savedEntity);
        } catch (Exception e) {
            log.warn("WelfareSyncService.fetchAndSaveWelfareDetailByServiceId failed. serviceId={}, reason={}",
                    serviceId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private int markDiscontinuedServices(Set<String> currentServiceIds, LocalDateTime syncStartedAt) {
        final int[] discontinuedCount = {0};
        transactionTemplate.executeWithoutResult(status -> {
            List<WelfareServiceEntity> activeServices = welfareServiceRepository.findAllByActiveTrue();

            for (WelfareServiceEntity activeService : activeServices) {
                if (!currentServiceIds.contains(activeService.getServiceId())) {
                    activeService.markDiscontinued(syncStartedAt);
                    discontinuedCount[0]++;
                }
            }

            log.info("WelfareSyncService.markDiscontinuedServices completed. activeCount={}, discontinuedCount={}",
                    activeServices.size(), discontinuedCount[0]);
        });
        return discontinuedCount[0];
    }

}
