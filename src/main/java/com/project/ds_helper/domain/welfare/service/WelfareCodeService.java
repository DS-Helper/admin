package com.project.ds_helper.domain.welfare.service;

import com.project.ds_helper.domain.welfare.dto.response.WelfareCodeGroupResponse;
import com.project.ds_helper.domain.welfare.dto.response.WelfareCodeItemResponse;
import com.project.ds_helper.domain.welfare.dto.response.WelfareCodeSeedResponse;
import com.project.ds_helper.domain.welfare.dto.response.WelfareCodeRefResponse;
import com.project.ds_helper.domain.welfare.entity.WelfareCodeEntity;
import com.project.ds_helper.domain.welfare.entity.WelfareCodeGroup;
import com.project.ds_helper.domain.welfare.repository.WelfareCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WelfareCodeService {

    private final WelfareCodeRepository welfareCodeRepository;

    public List<WelfareCodeEntity> getActiveCodes(WelfareCodeGroup codeGroup) {
        return welfareCodeRepository.findAllByCodeGroupAndActiveTrueOrderBySortOrderAsc(codeGroup);
    }

    public List<WelfareCodeEntity> getAllCodes(WelfareCodeGroup codeGroup) {
        return welfareCodeRepository.findAllByCodeGroupOrderBySortOrderAsc(codeGroup);
    }

    public Optional<WelfareCodeEntity> findActiveCode(WelfareCodeGroup codeGroup, String codeValue) {
        return welfareCodeRepository.findByCodeGroupAndCodeValueAndActiveTrue(codeGroup, codeValue);
    }

    public boolean existsActiveCode(WelfareCodeGroup codeGroup, String codeValue) {
        return findActiveCode(codeGroup, codeValue).isPresent();
    }

    public String resolveCodeName(WelfareCodeGroup codeGroup, String codeValue) {
        return findActiveCode(codeGroup, codeValue)
                .map(WelfareCodeEntity::getCodeNameKo)
                .orElse(codeValue);
    }

    public String resolveLifeCycleCode(int age) {
        if (age >= 0 && age <= 5) {
            return "001";
        }
        if (age <= 12) {
            return "002";
        }
        if (age <= 18) {
            return "003";
        }
        if (age <= 39) {
            return "004";
        }
        if (age <= 64) {
            return "005";
        }
        return "006";
    }

    public String resolveCodeNames(WelfareCodeGroup codeGroup, String rawCodes) {
        if (rawCodes == null || rawCodes.isBlank()) {
            return rawCodes;
        }
        return Arrays.stream(rawCodes.split("[,\\s]+"))
                .filter(code -> !code.isBlank())
                .map(code -> resolveCodeName(codeGroup, code))
                .collect(Collectors.joining(", "));
    }

    public List<WelfareCodeRefResponse> getCodeRefs(WelfareCodeGroup codeGroup) {
        return getActiveCodes(codeGroup).stream()
                .map(code -> WelfareCodeRefResponse.builder()
                        .codeGroup(code.getCodeGroup().name())
                        .codeValue(code.getCodeValue())
                        .codeNameKo(code.getCodeNameKo())
                        .build())
                .toList();
    }

    public List<WelfareCodeGroupResponse> getAllActiveCodesGrouped() {
        return Arrays.stream(WelfareCodeGroup.values())
                .map(codeGroup -> WelfareCodeGroupResponse.builder()
                        .codeGroup(codeGroup)
                        .codes(getActiveCodes(codeGroup).stream().map(WelfareCodeItemResponse::from).toList())
                        .build())
                .toList();
    }

    @Transactional
    public WelfareCodeSeedResponse seedDefaultCodes() {
        List<WelfareCodeEntity> codes = defaultCodes();
        welfareCodeRepository.saveAll(codes);
        return WelfareCodeSeedResponse.builder()
                .upsertedCount(codes.size())
                .groupCount((int) codes.stream().map(WelfareCodeEntity::getCodeGroup).distinct().count())
                .build();
    }

    private List<WelfareCodeEntity> defaultCodes() {
        List<WelfareCodeEntity> codes = new ArrayList<>();

        codes.add(code(WelfareCodeGroup.LIFE_ARRAY, "001", "영유아", 1));
        codes.add(code(WelfareCodeGroup.LIFE_ARRAY, "002", "아동", 2));
        codes.add(code(WelfareCodeGroup.LIFE_ARRAY, "003", "청소년", 3));
        codes.add(code(WelfareCodeGroup.LIFE_ARRAY, "004", "청년", 4));
        codes.add(code(WelfareCodeGroup.LIFE_ARRAY, "005", "중장년", 5));
        codes.add(code(WelfareCodeGroup.LIFE_ARRAY, "006", "노년", 6));
        codes.add(code(WelfareCodeGroup.LIFE_ARRAY, "007", "임신출산", 7));

        codes.add(code(WelfareCodeGroup.TARGET_INDV_ARRAY, "010", "다문화탈북민", 1));
        codes.add(code(WelfareCodeGroup.TARGET_INDV_ARRAY, "020", "다자녀", 2));
        codes.add(code(WelfareCodeGroup.TARGET_INDV_ARRAY, "030", "보훈대상자", 3));
        codes.add(code(WelfareCodeGroup.TARGET_INDV_ARRAY, "040", "장애인", 4));
        codes.add(code(WelfareCodeGroup.TARGET_INDV_ARRAY, "050", "저소득", 5));
        codes.add(code(WelfareCodeGroup.TARGET_INDV_ARRAY, "060", "한부모조손", 6));

        codes.add(code(WelfareCodeGroup.INTEREST_THEME_ARRAY, "010", "신체건강", 1));
        codes.add(code(WelfareCodeGroup.INTEREST_THEME_ARRAY, "020", "정신건강", 2));
        codes.add(code(WelfareCodeGroup.INTEREST_THEME_ARRAY, "030", "생활지원", 3));
        codes.add(code(WelfareCodeGroup.INTEREST_THEME_ARRAY, "040", "주거", 4));
        codes.add(code(WelfareCodeGroup.INTEREST_THEME_ARRAY, "050", "일자리", 5));
        codes.add(code(WelfareCodeGroup.INTEREST_THEME_ARRAY, "060", "문화여가", 6));
        codes.add(code(WelfareCodeGroup.INTEREST_THEME_ARRAY, "070", "안전위기", 7));
        codes.add(code(WelfareCodeGroup.INTEREST_THEME_ARRAY, "080", "임신출산", 8));
        codes.add(code(WelfareCodeGroup.INTEREST_THEME_ARRAY, "090", "보육", 9));
        codes.add(code(WelfareCodeGroup.INTEREST_THEME_ARRAY, "100", "교육", 10));
        codes.add(code(WelfareCodeGroup.INTEREST_THEME_ARRAY, "110", "입양위탁", 11));
        codes.add(code(WelfareCodeGroup.INTEREST_THEME_ARRAY, "120", "보호돌봄", 12));
        codes.add(code(WelfareCodeGroup.INTEREST_THEME_ARRAY, "130", "서민금융", 13));
        codes.add(code(WelfareCodeGroup.INTEREST_THEME_ARRAY, "140", "법률", 14));

        codes.add(code(WelfareCodeGroup.WELFARE_INFO_DETAIL_CODE, "010", "문의", 1));
        codes.add(code(WelfareCodeGroup.WELFARE_INFO_DETAIL_CODE, "020", "사이트", 2));
        codes.add(code(WelfareCodeGroup.WELFARE_INFO_DETAIL_CODE, "030", "근거법령", 3));
        codes.add(code(WelfareCodeGroup.WELFARE_INFO_DETAIL_CODE, "040", "서식자료", 4));

        codes.add(code(WelfareCodeGroup.SEARCH_KEY_CODE, "001", "서비스명", 1));
        codes.add(code(WelfareCodeGroup.SEARCH_KEY_CODE, "002", "서비스내용", 2));
        codes.add(code(WelfareCodeGroup.SEARCH_KEY_CODE, "003", "서비스명+서비스내용", 3));

        codes.add(code(WelfareCodeGroup.ARRANGE_ORDER, "001", "최신순", 1));
        codes.add(code(WelfareCodeGroup.ARRANGE_ORDER, "002", "인기순", 2));

        return codes;
    }

    private WelfareCodeEntity code(WelfareCodeGroup group, String value, String name, Integer sortOrder) {
        String codeId = group.name() + "_" + value;
        return WelfareCodeEntity.builder()
                .codeId(codeId)
                .codeGroup(group)
                .codeValue(value)
                .codeNameKo(name)
                .normalizedName(normalize(name))
                .sortOrder(sortOrder)
                .active(true)
                .build();
    }

    private String normalize(String value) {
        return value == null ? null : value.replaceAll("\\s+", "");
    }
}
