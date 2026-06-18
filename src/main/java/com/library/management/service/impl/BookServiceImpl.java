package com.library.management.service.impl;

import com.library.management.domain.entity.Author;
import com.library.management.domain.entity.Book;
import com.library.management.dto.external.OpenLibraryBookDto;
import com.library.management.dto.request.CreateBookRequest;
import com.library.management.dto.response.BookResponse;
import com.library.management.dto.response.PageResponse;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ErrorCode;
import com.library.management.mapper.BookMapper;
import com.library.management.repository.AuthorRepository;
import com.library.management.repository.BookRepository;
import com.library.management.service.BookService;
import com.library.management.service.IsbnLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final BookMapper bookMapper;
    private final IsbnLookupService isbnLookupService;

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "title", "publicationYear", "genre");

    @Override
    public BookResponse create(CreateBookRequest request) {
        Author author = authorRepository.findById(request.authorId())
                .orElseThrow(() -> BusinessException.notFound("Author", request.authorId()));

        Book book = bookMapper.toEntity(request);
        book.setAuthor(author);
        book.setAvailableCopies(request.totalCopies());

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
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Invalid sort field '" + sortBy + "'. Allowed values: " + ALLOWED_SORT_FIELDS,
                    HttpStatus.BAD_REQUEST
            );
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<Book> result = bookRepository.searchBooks(title, authorName, genre, pageable);
        return PageResponse.from(result.map(bookMapper::toResponse));
    }

    @Override
    public BookResponse update(Long id, CreateBookRequest request) {
        Book book = findById(id);

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

    @Override
    public BookResponse createByIsbn(String isbn, Long authorId, Integer totalCopies) {
        if (bookRepository.existsByIsbn(isbn)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE,
                    "A book with ISBN " + isbn + " already exists in the library",
                    HttpStatus.CONFLICT);
        }

        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> BusinessException.notFound("Author", authorId));

        OpenLibraryBookDto dto = isbnLookupService.fetchByIsbn(isbn);

        if (dto == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "No book found for ISBN: " + isbn +
                            ". Please use the manual create endpoint instead.",
                    HttpStatus.NOT_FOUND);
        }

        int publicationYear = extractYear(dto.publishDate());

        Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(dto.title());
        book.setAuthor(author);
        book.setPublicationYear(publicationYear);
        book.setTotalCopies(totalCopies != null ? totalCopies : 1);
        book.setAvailableCopies(book.getTotalCopies());

        Book saved = bookRepository.save(book);

        log.info("Book created via ISBN lookup: id={} title='{}' isbn={}",
                saved.getId(), saved.getTitle(), isbn);

        return bookMapper.toResponse(saved);
    }

    private int extractYear(String publishDate) {
        if (publishDate == null || publishDate.isBlank()) {
            return LocalDate.now().getYear();
        }
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern
                        .compile("(1[89]\\d{2}|20\\d{2})")
                        .matcher(publishDate);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        log.warn("Could not parse year from publishDate='{}', using current year",
                publishDate);
        return LocalDate.now().getYear();
    }
}