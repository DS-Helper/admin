package com.project.ds_helper.domain.welfare.service;

import com.project.ds_helper.domain.welfare.entity.WelfareCodeEntity;
import com.project.ds_helper.domain.welfare.entity.WelfareCodeGroup;
import com.project.ds_helper.domain.welfare.repository.WelfareCodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WelfareCodeServiceTest {

    @Mock private WelfareCodeRepository welfareCodeRepository;

    @Test
    @DisplayName("활성 코드가 있으면 코드명을 반환한다")
    void resolveCodeName_returnsCodeName() {
        WelfareCodeService service = new WelfareCodeService(welfareCodeRepository);
        WelfareCodeEntity code = WelfareCodeEntity.builder().codeNameKo("영유아").build();
        when(welfareCodeRepository.findByCodeGroupAndCodeValueAndActiveTrue(WelfareCodeGroup.LIFE_ARRAY, "001"))
                .thenReturn(Optional.of(code));

        assertThat(service.resolveCodeName(WelfareCodeGroup.LIFE_ARRAY, "001")).isEqualTo("영유아");
    }

    @Test
    @DisplayName("활성 코드가 없으면 원래 코드를 반환한다")
    void resolveCodeName_returnsOriginalWhenMissing() {
        WelfareCodeService service = new WelfareCodeService(welfareCodeRepository);
        when(welfareCodeRepository.findByCodeGroupAndCodeValueAndActiveTrue(WelfareCodeGroup.LIFE_ARRAY, "999"))
                .thenReturn(Optional.empty());

        assertThat(service.resolveCodeName(WelfareCodeGroup.LIFE_ARRAY, "999")).isEqualTo("999");
    }

    @Test
    @DisplayName("코드 시드는 기본 코드 묶음을 저장한다")
    void seedDefaultCodes_savesAllCodes() {
        WelfareCodeService service = new WelfareCodeService(welfareCodeRepository);

        var result = service.seedDefaultCodes();

        assertThat(result.upsertedCount()).isGreaterThan(0);
        assertThat(result.groupCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("활성 코드 목록과 전체 코드 목록을 조회한다")
    void getCodes_returnsRepositoryResults() {
        WelfareCodeService service = new WelfareCodeService(welfareCodeRepository);
        WelfareCodeEntity code = WelfareCodeEntity.builder().codeNameKo("영유아").build();
        when(welfareCodeRepository.findAllByCodeGroupAndActiveTrueOrderBySortOrderAsc(WelfareCodeGroup.LIFE_ARRAY)).thenReturn(List.of(code));
        when(welfareCodeRepository.findAllByCodeGroupOrderBySortOrderAsc(WelfareCodeGroup.LIFE_ARRAY)).thenReturn(List.of(code));

        assertThat(service.getActiveCodes(WelfareCodeGroup.LIFE_ARRAY)).hasSize(1);
        assertThat(service.getAllCodes(WelfareCodeGroup.LIFE_ARRAY)).hasSize(1);
    }

    @Test
    @DisplayName("활성 코드가 존재하는지 확인한다")
    void existsActiveCode_returnsTrueWhenPresent() {
        WelfareCodeService service = new WelfareCodeService(welfareCodeRepository);
        when(welfareCodeRepository.findByCodeGroupAndCodeValueAndActiveTrue(WelfareCodeGroup.LIFE_ARRAY, "001"))
                .thenReturn(Optional.of(WelfareCodeEntity.builder().codeNameKo("영유아").build()));

        assertThat(service.existsActiveCode(WelfareCodeGroup.LIFE_ARRAY, "001")).isTrue();
    }

    @Test
    @DisplayName("활성 코드명이 없으면 코드값을 원문으로 반환한다")
    void resolveCodeName_returnsOriginalWhenRepositoryMissAndNullCode() {
        WelfareCodeService service = new WelfareCodeService(welfareCodeRepository);
        when(welfareCodeRepository.findByCodeGroupAndCodeValueAndActiveTrue(WelfareCodeGroup.LIFE_ARRAY, null))
                .thenReturn(Optional.empty());

        assertThat(service.resolveCodeName(WelfareCodeGroup.LIFE_ARRAY, null)).isNull();
    }

    @Test
    @DisplayName("복수 코드명은 쉼표 기준으로 모두 변환한다")
    void resolveCodeNames_resolvesMultipleCodes() {
        WelfareCodeService service = new WelfareCodeService(welfareCodeRepository);
        when(welfareCodeRepository.findByCodeGroupAndCodeValueAndActiveTrue(WelfareCodeGroup.INTEREST_THEME_ARRAY, "010"))
                .thenReturn(Optional.of(WelfareCodeEntity.builder().codeNameKo("신체건강").build()));
        when(welfareCodeRepository.findByCodeGroupAndCodeValueAndActiveTrue(WelfareCodeGroup.INTEREST_THEME_ARRAY, "020"))
                .thenReturn(Optional.of(WelfareCodeEntity.builder().codeNameKo("정신건강").build()));

        assertThat(service.resolveCodeNames(WelfareCodeGroup.INTEREST_THEME_ARRAY, "010, 020")).isEqualTo("신체건강, 정신건강");
    }

    @Test
    @DisplayName("코드 참조 목록과 그룹별 코드 목록을 반환한다")
    void getCodeRefs_and_getAllActiveCodesGrouped_returnMappedValues() {
        WelfareCodeService service = new WelfareCodeService(welfareCodeRepository);
        WelfareCodeEntity code = WelfareCodeEntity.builder()
                .codeGroup(WelfareCodeGroup.LIFE_ARRAY)
                .codeValue("001")
                .codeNameKo("영유아")
                .sortOrder(1)
                .active(true)
                .build();
        when(welfareCodeRepository.findAllByCodeGroupAndActiveTrueOrderBySortOrderAsc(WelfareCodeGroup.LIFE_ARRAY)).thenReturn(List.of(code));
        when(welfareCodeRepository.findAllByCodeGroupAndActiveTrueOrderBySortOrderAsc(WelfareCodeGroup.TARGET_INDV_ARRAY)).thenReturn(List.of());
        when(welfareCodeRepository.findAllByCodeGroupAndActiveTrueOrderBySortOrderAsc(WelfareCodeGroup.INTEREST_THEME_ARRAY)).thenReturn(List.of());
        when(welfareCodeRepository.findAllByCodeGroupAndActiveTrueOrderBySortOrderAsc(WelfareCodeGroup.WELFARE_INFO_DETAIL_CODE)).thenReturn(List.of());
        when(welfareCodeRepository.findAllByCodeGroupAndActiveTrueOrderBySortOrderAsc(WelfareCodeGroup.SEARCH_KEY_CODE)).thenReturn(List.of());
        when(welfareCodeRepository.findAllByCodeGroupAndActiveTrueOrderBySortOrderAsc(WelfareCodeGroup.ARRANGE_ORDER)).thenReturn(List.of());

        assertThat(service.getCodeRefs(WelfareCodeGroup.LIFE_ARRAY)).hasSize(1);
        assertThat(service.getAllActiveCodesGrouped()).isNotEmpty();
    }

    @Test
    @DisplayName("생애주기 코드는 연령대별로 구분한다")
    void resolveLifeCycleCode_mapsAgeRanges() {
        WelfareCodeService service = new WelfareCodeService(welfareCodeRepository);

        assertThat(service.resolveLifeCycleCode(0)).isEqualTo("001");
        assertThat(service.resolveLifeCycleCode(6)).isEqualTo("002");
        assertThat(service.resolveLifeCycleCode(13)).isEqualTo("003");
        assertThat(service.resolveLifeCycleCode(25)).isEqualTo("004");
        assertThat(service.resolveLifeCycleCode(50)).isEqualTo("005");
        assertThat(service.resolveLifeCycleCode(80)).isEqualTo("006");
    }
}
