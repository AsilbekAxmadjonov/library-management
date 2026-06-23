package com.library.management.service.impl;

import com.library.management.domain.entity.Author;
import com.library.management.dto.request.CreateAuthorRequest;
import com.library.management.dto.response.AuthorResponse;
import com.library.management.dto.response.PageResponse;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ErrorCode;
import com.library.management.mapper.AuthorMapper;
import com.library.management.repository.AuthorRepository;
import com.library.management.service.AuthorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
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
        log.info("create author requested: firstName={} lastName={}", request.firstName(), request.lastName());

        if (authorRepository.existsByFirstNameAndLastName(
                request.firstName(), request.lastName())) {
            log.warn("Duplicate author rejected: {} {}", request.firstName(), request.lastName());
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
        log.debug("getById author: id={}", id);
        return authorMapper.toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuthorResponse> getAll(Pageable pageable) {
        log.debug("getAll authors: page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
        return PageResponse.from(authorRepository.findAll(pageable)
                .map(authorMapper::toResponse));
    }

    @Override
    public AuthorResponse update(Long id, CreateAuthorRequest request) {
        log.info("update author requested: id={}", id);
        Author author = findById(id);
        authorMapper.updateEntity(request, author);
        Author saved = authorRepository.save(author);
        log.info("Author updated: id={} name={} {}", saved.getId(), saved.getFirstName(), saved.getLastName());
        return authorMapper.toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        log.info("delete author requested: id={}", id);
        Author author = findById(id);
        authorRepository.delete(author);
        log.info("Author deleted: id={}", id);
    }

    private Author findById(Long id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Author", id));
    }
}
