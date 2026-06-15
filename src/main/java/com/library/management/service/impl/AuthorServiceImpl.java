package com.library.management.service.impl;

import com.library.management.domain.entity.Author;
import com.library.management.dto.request.CreateAuthorRequest;
import com.library.management.dto.response.AuthorResponse;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ErrorCode;
import com.library.management.mapper.AuthorMapper;
import com.library.management.repository.AuthorRepository;
import com.library.management.service.AuthorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;
    private final AuthorMapper authorMapper;

    @Override
    public AuthorResponse create(CreateAuthorRequest request) {
        if (authorRepository.existsByFirstNameAndLastName(
                request.firstName(), request.lastName())) {
            throw new BusinessException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "Author already exists: " + request.firstName() + " " + request.lastName(),
                    HttpStatus.CONFLICT
            );
        }

        Author author = authorMapper.toEntity(request);
        Author saved = authorRepository.save(author);
        log.info("Author created: id={}", saved.getId());
        return authorMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthorResponse getById(Long id) {
        return authorMapper.toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuthorResponse> getAll() {
        return authorRepository.findAll()
                .stream()
                .map(authorMapper::toResponse)
                .toList();
    }

    @Override
    public AuthorResponse update(Long id, CreateAuthorRequest request) {
        Author author = findById(id);
        authorMapper.updateEntity(request, author);
        return authorMapper.toResponse(authorRepository.save(author));
    }

    @Override
    public void delete(Long id) {
        Author author = findById(id);
        authorRepository.delete(author);
        log.info("Author deleted: id={}", id);
    }

    private Author findById(Long id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Author", id));
    }
}
