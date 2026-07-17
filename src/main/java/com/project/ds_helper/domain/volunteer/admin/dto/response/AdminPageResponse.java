package com.project.ds_helper.domain.volunteer.admin.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** 관리자 목록의 공통 페이지 계약입니다. */
public record AdminPageResponse<T>(List<T> content, PageInfo page) {
    public static <S, T> AdminPageResponse<T> from(Page<S> source, Function<S, T> mapper) {
        return new AdminPageResponse<>(
                source.getContent().stream().map(mapper).toList(),
                new PageInfo(source.getNumber(), source.getSize(), source.getTotalElements(), source.getTotalPages(), source.hasNext())
        );
    }

    public record PageInfo(int page, int size, long totalElements, int totalPages, boolean hasNext) { }
}
