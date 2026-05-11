package com.project.ds_helper.domain.reservation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;

import java.util.List;

@Slf4j
@Schema(
        description = "기관 예약 목록 슬라이스 DTO",
        example = "{\"content\":[],\"number\":0,\"size\":10,\"first\":true,\"last\":false,\"hasNext\":true}"
)
public record GetOrganizationReservationsResDto<T>(
        List<T> content,
        int number,
        int size,
        boolean first,
        boolean last,
        boolean hasNext
) {
    public static <T> GetOrganizationReservationsResDto<T> toDto(Slice<T> sliceObject) {
        return new GetOrganizationReservationsResDto<>(
                sliceObject.getContent(),
                Math.max(0, sliceObject.getNumber()),
                Math.max(0, sliceObject.getSize()),
                sliceObject.isFirst(),
                sliceObject.isLast(),
                sliceObject.hasNext()
        );
    }
}
