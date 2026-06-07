package com.library.management.service;

import com.library.management.dto.external.OpenLibraryBookDto;

public interface IsbnLookupService {

    // Fetches book metadata from Open Library by ISBN
    // Returns null if ISBN not found
    OpenLibraryBookDto fetchByIsbn(String isbn);
}