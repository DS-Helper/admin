package com.project.ds_helper.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

@Schema(description = "페이지네이션 메타 정보 DTO", example = "{\"page\":0,\"size\":10,\"totalElements\":125,\"totalPages\":13,\"first\":true,\"last\":false,\"hasNext\":true,\"hasPrevious\":false,\"sort\":{\"sorted\":true,\"unsorted\":false,\"empty\":false}}")
public record PageResponseDto(
        int page,               // 0-based (Slice일 땐 호출자가 0 고정으로 넣어도 OK)
        int size,
        long totalElements,     // Slice면 -1 또는 0
        int totalPages,         // Slice면 -1
        boolean first,
        boolean last,
        boolean hasNext,
        boolean hasPrevious,
        Sort sort    // 정렬 정보 에코백
) {

    public static PageResponseDto toDto(Page<?> page){
        return new PageResponseDto(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious(),
                page.getSort());
    }
}
