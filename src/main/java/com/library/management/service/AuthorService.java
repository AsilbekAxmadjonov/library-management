package com.library.management.service;

import com.library.management.dto.request.CreateAuthorRequest;
import com.library.management.dto.response.AuthorResponse;
import com.library.management.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AuthorService {

    AuthorResponse create(CreateAuthorRequest request);

    AuthorResponse getById(Long id);

    PageResponse<AuthorResponse> getAll(Pageable pageable);

    AuthorResponse update(Long id, CreateAuthorRequest request);

    void delete(Long id);
}
