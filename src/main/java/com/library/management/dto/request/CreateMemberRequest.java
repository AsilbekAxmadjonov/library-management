package com.library.management.dto.request;

import com.library.management.domain.enums.MemberType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMemberRequest(
        @NotBlank @Size(max = 100)           String firstName,
        @NotBlank @Size(max = 100)           String lastName,
        @NotBlank @Email @Size(max = 150)    String email,
        @Size(max = 20)                      String phone,
        MemberType type      // optional, defaults to STANDARD
) {}