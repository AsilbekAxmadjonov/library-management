package com.library.management.dto.request;

import jakarta.validation.constraints.NotNull;

public record IssueLoanRequest(
        @NotNull Long memberId,
        @NotNull Long bookId
) {}

