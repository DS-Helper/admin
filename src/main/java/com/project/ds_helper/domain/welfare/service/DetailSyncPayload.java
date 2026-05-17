package com.project.ds_helper.domain.welfare.service;

import java.time.LocalDateTime;

record DetailSyncPayload(
        boolean success,
        boolean detailSkipped,
        String targetDetailContent,
        String benefitContent,
        String selectionCriteriaContent,
        String applicationMethodList,
        String homepageUrl,
        String inquiryContactList,
        String homepageList,
        String basisLawList,
        String basisFormList,
        LocalDateTime detailSyncedAt,
        String failureReason
) {
    static DetailSyncPayload success(
            String targetDetailContent,
            String benefitContent,
            String selectionCriteriaContent,
            String applicationMethodList,
            String homepageUrl,
            String inquiryContactList,
            String homepageList,
            String basisLawList,
            String basisFormList,
            LocalDateTime detailSyncedAt
    ) {
        return new DetailSyncPayload(
                true,
                false,
                targetDetailContent,
                benefitContent,
                selectionCriteriaContent,
                applicationMethodList,
                homepageUrl,
                inquiryContactList,
                homepageList,
                basisLawList,
                basisFormList,
                detailSyncedAt,
                null
        );
    }

    static DetailSyncPayload failed(String failureReason) {
        return new DetailSyncPayload(false, false, null, null, null, null, null, null, null, null, null, null, failureReason);
    }

    static DetailSyncPayload skipped() {
        return new DetailSyncPayload(false, true, null, null, null, null, null, null, null, null, null, null, null);
    }
}
