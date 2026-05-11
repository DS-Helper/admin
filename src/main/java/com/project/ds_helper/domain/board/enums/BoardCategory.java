package com.project.ds_helper.domain.board.enums;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Getter
public enum BoardCategory {

    FREE("자유"),
    DAILY("일상"),
    HEALTH("건강"),
    PARENTING("육아"),
    EDUCATION("교육"),
    COMMERCIAL("상권"),
    CULTURE_LEISURE("문화/여가"),
    ETC("기타");

    private final String korean;

    private static final Map<String, BoardCategory> KOREAN_MAP =
            Stream.of(values())
                    .collect(Collectors.toMap(BoardCategory::getKorean, e -> e));

    BoardCategory(String korean) {
        this.korean = korean;
    }

    public static BoardCategory findByKorean(String korean) {
        log.debug("Given Korean : {}", korean);

        BoardCategory category = KOREAN_MAP.get(korean);

        if (category == null) {
            throw new IllegalArgumentException("Invalid BoardCategory : " + korean);
        }

        return category;
    }
}
