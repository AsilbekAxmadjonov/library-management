package com.library.management.dto.response;

import com.library.management.domain.enums.LoanStatus;

import java.time.LocalDate;

public record LoanResponse(
        Long id,
        Long memberId,
        String memberFullName,
        Long bookId,
        String bookTitle,
        LocalDate loanDate,
        LocalDate dueDate,
        LocalDate returnDate,
        LoanStatus status,
        int extensionCount
) {}
