package com.project.ds_helper.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Slice;

import java.util.List;

@Schema(
        description = "슬라이스 기반 페이징 응답 DTO",
        example = "{\"content\":[],\"number\":0,\"size\":10,\"first\":true,\"last\":false,\"hasNext\":true}"
)
public record SliceResponseDto<T>(
        List<T> content,
        int number,
        int size,
        boolean first,
        boolean last,
        boolean hasNext
) {
    public static <T> SliceResponseDto<T> toDto(Slice<T> sliceObject) {
        return new SliceResponseDto<>(
                sliceObject.getContent(),
                Math.max(0, sliceObject.getNumber()),
                Math.max(0, sliceObject.getSize()),
                sliceObject.isFirst(),
                sliceObject.isLast(),
                sliceObject.hasNext()
        );
    }
}
