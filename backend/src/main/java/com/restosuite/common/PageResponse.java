package com.restosuite.common;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(List<T> items, long total, int page, int size) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getTotalElements(), page.getNumber(), page.getSize());
    }
}
