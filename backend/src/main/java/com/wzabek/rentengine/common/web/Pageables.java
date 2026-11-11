package com.wzabek.rentengine.common.web;

import com.wzabek.rentengine.common.exception.InvalidRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

public final class Pageables {

    private Pageables() {
    }

    // Nieznane pole sortowania skończyłoby się błędem 500 w zapytaniu, więc puszczamy tylko dozwolone.
    // Dosortowanie po id daje stabilną kolejność między stronami przy równych wartościach.
    public static Pageable withAllowedSort(Pageable pageable, List<String> allowed) {
        pageable.getSort().stream()
                .filter(order -> !allowed.contains(order.getProperty()))
                .findFirst()
                .ifPresent(order -> {
                    throw new InvalidRequestException(Map.of("sort",
                            "unsupported property '%s', allowed: %s".formatted(order.getProperty(), allowed)));
                });
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().and(Sort.by("id")));
    }
}
