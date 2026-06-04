package com.library.management.dto.response;

public record FineStatsResponse(
        long totalFines,
        long totalAmount,
        long paidAmount,
        long unpaidAmount
) {}
