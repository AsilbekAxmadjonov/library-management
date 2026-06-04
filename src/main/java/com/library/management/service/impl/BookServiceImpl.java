package com.library.management.service.impl;

import com.library.management.domain.entity.Author;
import com.library.management.domain.entity.Book;
import com.library.management.dto.request.CreateBookRequest;
import com.library.management.dto.response.BookResponse;
import com.library.management.dto.response.PageResponse;
import com.library.management.exception.BusinessException;
import com.library.management.mapper.BookMapper;
import com.library.management.repository.AuthorRepository;
import com.library.management.repository.BookRepository;
import com.library.management.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// service/impl/BookServiceImpl.java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final BookMapper bookMapper;

    @Override
    public BookResponse create(CreateBookRequest request) {
        Author author = authorRepository.findById(request.authorId())
                .orElseThrow(() -> BusinessException.notFound("Author", request.authorId()));

        Book book = bookMapper.toEntity(request);
        book.setAuthor(author);
        book.setAvailableCopies(request.totalCopies()); // all copies available on create

        Book saved = bookRepository.save(book);
        log.info("Book created: id={} title={}", saved.getId(), saved.getTitle());
        return bookMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BookResponse getById(Long id) {
        return bookMapper.toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookResponse> search(String title, String authorName,
                                             String genre, int page, int size,
                                             String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<Book> result = bookRepository.searchBooks(title, authorName, genre, pageable);
        return PageResponse.from(result.map(bookMapper::toResponse));
    }

    @Override
    public BookResponse update(Long id, CreateBookRequest request) {
        Book book = findById(id);

        // Adjust availableCopies by the delta if totalCopies changed
        int delta = request.totalCopies() - book.getTotalCopies();
        book.setAvailableCopies(Math.max(0, book.getAvailableCopies() + delta));

        Author author = authorRepository.findById(request.authorId())
                .orElseThrow(() -> BusinessException.notFound("Author", request.authorId()));

        bookMapper.updateEntity(request, book);
        book.setAuthor(author);

        return bookMapper.toResponse(bookRepository.save(book));
    }

    @Override
    public void delete(Long id) {
        bookRepository.delete(findById(id));
        log.info("Book deleted: id={}", id);
    }

    private Book findById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Book", id));
    }
}
