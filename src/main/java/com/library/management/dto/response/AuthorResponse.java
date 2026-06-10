package com.library.management.dto.response;

import java.time.LocalDateTime;

public record AuthorResponse(
        Long id,
        String firstName,
        String lastName,
        String bio,
        LocalDateTime createdAt
) {}
