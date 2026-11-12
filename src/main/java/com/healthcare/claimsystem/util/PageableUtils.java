package com.healthcare.claimsystem.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

public final class PageableUtils {

    private static final int MAX_PAGE_SIZE = 100;

    private PageableUtils() {
    }

    public static Pageable sanitize(Pageable pageable, Set<String> allowedSortFields, String defaultSortField) {
        List<Sort.Order> orders = pageable.getSort().stream()
                .filter(order -> allowedSortFields.contains(order.getProperty()))
                .map(order -> new Sort.Order(order.getDirection(), order.getProperty()))
                .toList();
        Sort sort = orders.isEmpty()
                ? Sort.by(Sort.Direction.DESC, defaultSortField)
                : Sort.by(orders);
        int pageNumber = Math.max(pageable.getPageNumber(), 0);
        int pageSize = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        return PageRequest.of(pageNumber, pageSize, sort);
    }
}
