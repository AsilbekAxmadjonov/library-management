package com.library.management.dto.response;

public record BookResponse(
        Long id,
        String title,
        String isbn,
        Long authorId,
        String authorFullName,
        int totalCopies,
        int availableCopies,
        String genre,
        int publicationYear,
        Long price
) {}
