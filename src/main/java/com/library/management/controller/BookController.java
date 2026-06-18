package com.library.management.controller;

import com.library.management.dto.request.CreateBookRequest;
import com.library.management.dto.response.BookResponse;
import com.library.management.dto.response.PageResponse;
import com.library.management.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Validated
@Tag(name = "Books", description = "Manage books and search catalog")
public class BookController {

    private final BookService bookService;

    @PostMapping
    @Operation(summary = "Add a new book")
    public ResponseEntity<Void> create(@Valid @RequestBody CreateBookRequest request) {
        BookResponse created = bookService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/{book_id}")
    @Operation(summary = "Get book by ID")
    public ResponseEntity<BookResponse> getById(@PathVariable Long book_id) {
        return ResponseEntity.ok(bookService.getById(book_id));
    }

    @GetMapping
    @Operation(summary = "Search and filter books with pagination")
    public ResponseEntity<PageResponse<BookResponse>> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String authorName,
            @RequestParam(required = false) String genre,
            @RequestParam(defaultValue = "0")  @Min(0)            int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100)  int size,
            @RequestParam(defaultValue = "title")                 String sortBy
    ) {
        return ResponseEntity.ok(
                bookService.search(title, authorName, genre, page, size, sortBy));
    }

    @PutMapping("/{book_id}")
    @Operation(summary = "Update book by ID")
    public ResponseEntity<Void> update(
            @PathVariable Long book_id,
            @Valid @RequestBody CreateBookRequest request) {
        bookService.update(book_id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{book_id}")
    @Operation(summary = "Delete book by ID")
    public ResponseEntity<Void> delete(@PathVariable Long book_id) {
        bookService.delete(book_id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/isbn/{isbn}")
    @Operation(
            summary = "Create book by ISBN lookup",
            description = "Fetches book metadata from Open Library API using the ISBN, " +
                    "then creates the book automatically. " +
                    "Only authorId and totalCopies need to be provided manually."
    )
    public ResponseEntity<Void> createByIsbn(
            @Parameter(description = "ISBN-10 or ISBN-13", example = "978-0132350884")
            @PathVariable String isbn,

            @Parameter(description = "ID of the author in our system", example = "1")
            @RequestParam Long authorId,

            @Parameter(description = "Number of physical copies (default: 1)")
            @RequestParam(required = false, defaultValue = "1") Integer totalCopies) {

        BookResponse created = bookService.createByIsbn(isbn, authorId, totalCopies);
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/books/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).build();
    }
}