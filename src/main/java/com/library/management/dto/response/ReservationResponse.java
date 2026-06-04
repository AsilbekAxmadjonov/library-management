package com.library.management.dto.response;

import com.library.management.domain.enums.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long memberId,
        String memberFullName,
        Long bookId,
        String bookTitle,
        LocalDateTime reservedAt,
        ReservationStatus status,
        LocalDate expiresAt
) {}
