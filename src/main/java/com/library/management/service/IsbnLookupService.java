package com.library.management.service;

import com.library.management.dto.external.OpenLibraryBookDto;

public interface IsbnLookupService {

    OpenLibraryBookDto fetchByIsbn(String isbn);
}