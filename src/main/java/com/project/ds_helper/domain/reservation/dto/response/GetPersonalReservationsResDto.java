package com.project.ds_helper.domain.reservation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;

import java.util.List;

@Slf4j
@Schema(
        description = "개인 예약 목록 슬라이스 DTO",
        example = "{\"content\":[],\"number\":0,\"size\":10,\"first\":true,\"last\":false,\"hasNext\":true}"
)
public record GetPersonalReservationsResDto<T>(
        List<T> content,
        int number,
        int size,
        boolean first,
        boolean last,
        boolean hasNext
) {
    public static <T> GetPersonalReservationsResDto<T> toDto(Slice<T> sliceObject) {
        List<T> content = sliceObject.getContent();
        int number = Math.max(0, sliceObject.getNumber());
        int size = Math.max(0, sliceObject.getSize());
        boolean first = sliceObject.isFirst();
        boolean last = sliceObject.isLast();
        boolean hasNext = sliceObject.hasNext();

        log.info(
                "content.size : {}, number : {}, size : {}, first : {}, last : {}. hasNext : {}",
                content.size(),
                number,
                size,
                first,
                last,
                hasNext
        );

        return new GetPersonalReservationsResDto<>(content, number, size, first, last, hasNext);
    }
}
