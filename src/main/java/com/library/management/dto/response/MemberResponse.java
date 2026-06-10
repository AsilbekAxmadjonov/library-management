package com.library.management.dto.response;

import com.library.management.domain.enums.MemberStatus;
import com.library.management.domain.enums.MemberType;

import java.time.LocalDateTime;

public record MemberResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        MemberStatus status,
        MemberType type,
        LocalDateTime createdAt
) {}
