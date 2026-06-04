package com.library.management.dto.response;

import com.library.management.domain.enums.FineStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FineResponse(
        Long id,
        Long loanId,
        Long memberId,
        String memberFullName,
        Long amount,
        FineStatus status,
        LocalDate calculatedUpTo,
        LocalDateTime paidAt
) {}