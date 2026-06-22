package com.library.management.dto.projection;

public record FineStatsProjection(
        Long totalFines,
        Long totalAmount,
        Long paidAmount
) {}