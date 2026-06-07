package com.library.management.controller;

import com.library.management.dto.request.CreateBookRequest;
import com.library.management.dto.response.BookResponse;
import com.library.management.dto.response.PageResponse;
import com.library.management.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "Books", description = "Manage books and search catalog")
public class BookController {

    private final BookService bookService;

    @PostMapping
    @Operation(summary = "Add a new book")
    public ResponseEntity<BookResponse> create(
            @Valid @RequestBody CreateBookRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(bookService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID")
    public ResponseEntity<BookResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getById(id));
    }

    @GetMapping
    @Operation(summary = "Search and filter books with pagination")
    public ResponseEntity<PageResponse<BookResponse>> search(
            @Parameter(description = "Filter by title (partial match)")
            @RequestParam(required = false) String title,

            @Parameter(description = "Filter by author name (partial match)")
            @RequestParam(required = false) String authorName,

            @Parameter(description = "Filter by genre (exact match)")
            @RequestParam(required = false) String genre,

            @Parameter(description = "Page number, starts at 0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Sort field (e.g. title, publicationYear)")
            @RequestParam(defaultValue = "title") String sortBy) {
        return ResponseEntity.ok(
                bookService.search(title, authorName, genre, page, size, sortBy));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update book by ID")
    public ResponseEntity<BookResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateBookRequest request) {
        return ResponseEntity.ok(bookService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete book by ID")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/isbn/{isbn}")
    @Operation(
            summary = "Create book by ISBN lookup",
            description = "Fetches book metadata from Open Library API using the ISBN, " +
                    "then creates the book automatically. " +
                    "Only authorId and totalCopies need to be provided manually."
    )
    public ResponseEntity<BookResponse> createByIsbn(
            @Parameter(description = "ISBN-10 or ISBN-13", example = "978-0132350884")
            @PathVariable String isbn,

            @Parameter(description = "ID of the author in our system", example = "1")
            @RequestParam Long authorId,

            @Parameter(description = "Number of physical copies (default: 1)")
            @RequestParam(required = false, defaultValue = "1") Integer totalCopies) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(bookService.createByIsbn(isbn, authorId, totalCopies));
    }
}
