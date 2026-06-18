package com.library.management.controller;

import com.library.management.dto.request.CreateAuthorRequest;
import com.library.management.dto.response.AuthorResponse;
import com.library.management.dto.response.BaseResponse;
import com.library.management.dto.response.PageResponse;
import com.library.management.service.AuthorService;
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
@RequestMapping("/api/v1/authors")
@RequiredArgsConstructor
@Tag(name = "Authors", description = "Manage book authors")
public class AuthorController {

    private final AuthorService authorService;

    @PostMapping
    @Operation(summary = "Create a new author")
    public ResponseEntity<Void> create(@Valid @RequestBody CreateAuthorRequest request) {
        AuthorResponse created = authorService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/{author_id}")
    @Operation(summary = "Get author by ID")
    public BaseResponse<AuthorResponse> getById(@PathVariable Long author_id) {
        return BaseResponse.success(authorService.getById(author_id));
    }

    @GetMapping
    @Operation(summary = "Get all authors")
    public BaseResponse<PageResponse<AuthorResponse>> getAll(
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return BaseResponse.success(authorService.getAll(pageable));
    }

    @PutMapping("/{author_id}")
    @Operation(summary = "Update author by ID")
    public BaseResponse<Void> update(
            @PathVariable Long author_id,
            @Valid @RequestBody CreateAuthorRequest request) {
        authorService.update(author_id, request);
        return BaseResponse.success(null);
    }

    @DeleteMapping("/{author_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete author by ID")
    public void delete(@PathVariable Long author_id) {
        authorService.delete(author_id);
    }
}