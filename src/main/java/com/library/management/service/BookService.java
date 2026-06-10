package com.library.management.service;

import com.library.management.dto.request.CreateBookRequest;
import com.library.management.dto.response.BookResponse;
import com.library.management.dto.response.PageResponse;

// service/BookService.java
public interface BookService {

    BookResponse create(CreateBookRequest request);

    BookResponse getById(Long id);

    PageResponse<BookResponse> search(String title, String authorName,
                                      String genre, int page, int size,
                                      String sortBy);

    BookResponse update(Long id, CreateBookRequest request);

    void delete(Long id);

    BookResponse createByIsbn(String isbn, Long authorId, Integer totalCopies);
}