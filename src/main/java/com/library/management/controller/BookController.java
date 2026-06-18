package com.library.management.controller;

import com.library.management.dto.request.CreateBookRequest;
import com.library.management.dto.response.BaseResponse;
import com.library.management.dto.response.BookResponse;
import com.library.management.dto.response.PageResponse;
import com.library.management.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "Books", description = "Manage books")
public class BookController {

    private final BookService bookService;

    @PostMapping
    @Operation(summary = "Create a new book")
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
    public BaseResponse<BookResponse> getById(@PathVariable Long book_id) {
        return BaseResponse.success(bookService.getById(book_id));
    }

    @GetMapping
    @Operation(summary = "Get all books")
    public BaseResponse<PageResponse<BookResponse>> getAll(
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return BaseResponse.success(bookService.getAll(pageable));
    }

    @PutMapping("/{book_id}")
    @Operation(summary = "Update book by ID")
    public BaseResponse<Void> update(
            @PathVariable Long book_id,
            @Valid @RequestBody CreateBookRequest request) {
        bookService.update(book_id, request);
        return BaseResponse.success(null);
    }

    @DeleteMapping("/{book_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete book by ID")
    public void delete(@PathVariable Long book_id) {
        bookService.delete(book_id);
    }
}