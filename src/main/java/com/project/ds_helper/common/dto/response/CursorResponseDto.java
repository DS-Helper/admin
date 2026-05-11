package com.project.ds_helper.common.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(
        description = "커서 기반 페이징 응답 DTO",
        example = "{\"content\":[],\"cursorTime\":\"2026-03-23 14:30:00\",\"cursorId\":\"3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01\",\"hasNext\":true}"
)
public record CursorResponseDto<T>(
        List<T> content,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "다음 조회를 위한 커서 시간", example = "2026-03-23 14:30:00")
        LocalDateTime cursorTime,

        @Schema(description = "다음 조회를 위한 커서 ID. Comment 엔티티의 commentId와 동일한 UUID 형식 문자열입니다.", example = "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01")
        String cursorId,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {
    public static <T> CursorResponseDto<T> toDto(
            List<T> content,
            LocalDateTime cursorTime,
            String cursorId,
            boolean hasNext
    ) {
        return new CursorResponseDto<>(content, cursorTime, cursorId, hasNext);
    }
}
