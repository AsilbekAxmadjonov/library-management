package com.library.management.service;

import com.library.management.dto.request.CreateAuthorRequest;
import com.library.management.dto.response.AuthorResponse;

import java.util.List;

public interface AuthorService {

    AuthorResponse create(CreateAuthorRequest request);

    AuthorResponse getById(Long id);

    List<AuthorResponse> getAll();

    AuthorResponse update(Long id, CreateAuthorRequest request);

    void delete(Long id);
}
