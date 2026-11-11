package com.wzabek.rentengine.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

// Własny format strony zamiast PageImpl, żeby JSON nie zależał od wewnętrznej klasy Springa
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
