package com.library.management.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBookRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 20)            String isbn,
        @NotNull                   Long authorId,
        @NotNull @Min(1)           Integer totalCopies,
        @Size(max = 100)           String genre,
        @NotNull Integer publicationYear,
        Long price
) {}
