package com.library.management.service;

import com.library.management.domain.entity.Author;
import com.library.management.dto.request.CreateAuthorRequest;
import com.library.management.dto.response.AuthorResponse;
import com.library.management.dto.response.PageResponse;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ErrorCode;
import com.library.management.mapper.AuthorMapper;
import com.library.management.repository.AuthorRepository;
import com.library.management.service.impl.AuthorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorService unit tests")
class AuthorServiceTest {

    @Mock private AuthorRepository authorRepository;
    @Mock private AuthorMapper     authorMapper;

    @InjectMocks
    private AuthorServiceImpl authorService;

    private Author         author;
    private AuthorResponse authorResponse;

    @BeforeEach
    void setUp() {
        author = new Author();
        author.setId(1L);
        author.setFirstName("Robert");
        author.setLastName("Martin");

        authorResponse = new AuthorResponse(1L, "Robert", "Martin", null, null);
    }

    @Test
    @DisplayName("create — happy path: author saved and response returned")
    void create_happyPath_savesAuthor() {
        CreateAuthorRequest request = new CreateAuthorRequest("Robert", "Martin", null);

        when(authorRepository.existsByFirstNameAndLastName("Robert", "Martin")).thenReturn(false);
        when(authorMapper.toEntity(request)).thenReturn(author);
        when(authorRepository.save(author)).thenReturn(author);
        when(authorMapper.toResponse(author)).thenReturn(authorResponse);

        AuthorResponse result = authorService.create(request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.firstName()).isEqualTo("Robert");
        verify(authorRepository).save(author);
    }

    @Test
    @DisplayName("create — duplicate name → DUPLICATE_RESOURCE exception")
    void create_duplicateName_throwsException() {
        CreateAuthorRequest request = new CreateAuthorRequest("Robert", "Martin", null);

        when(authorRepository.existsByFirstNameAndLastName("Robert", "Martin")).thenReturn(true);

        assertThatThrownBy(() -> authorService.create(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_RESOURCE));

        verify(authorRepository, never()).save(any());
    }

    @Test
    @DisplayName("getById — existing id → returns response")
    void getById_exists_returnsResponse() {
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(authorMapper.toResponse(author)).thenReturn(authorResponse);

        AuthorResponse result = authorService.getById(1L);

        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getById — non-existing id → RESOURCE_NOT_FOUND exception")
    void getById_notFound_throwsException() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.getById(99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    @DisplayName("getAll — returns paged response")
    void getAll_returnsPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Author> page = new PageImpl<>(List.of(author), pageable, 1);

        when(authorRepository.findAll(pageable)).thenReturn(page);
        when(authorMapper.toResponse(author)).thenReturn(authorResponse);

        PageResponse<AuthorResponse> result = authorService.getAll(pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).lastName()).isEqualTo("Martin");
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(10);
    }

    @Test
    @DisplayName("update — happy path: entity updated and saved")
    void update_happyPath_updatesAuthor() {
        CreateAuthorRequest request = new CreateAuthorRequest("Robert", "Martin", "Clean Code author");

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(authorRepository.save(author)).thenReturn(author);
        when(authorMapper.toResponse(author)).thenReturn(authorResponse);

        AuthorResponse result = authorService.update(1L, request);

        assertThat(result).isNotNull();
        verify(authorMapper).updateEntity(request, author);
        verify(authorRepository).save(author);
    }

    @Test
    @DisplayName("update — non-existing id → RESOURCE_NOT_FOUND exception")
    void update_notFound_throwsException() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.update(99L,
                new CreateAuthorRequest("A", "B", null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    @DisplayName("delete — existing id: repository delete called")
    void delete_existing_deletesAuthor() {
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));

        authorService.delete(1L);

        verify(authorRepository).delete(author);
    }

    @Test
    @DisplayName("delete — non-existing id → RESOURCE_NOT_FOUND exception")
    void delete_notFound_throwsException() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.delete(99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }
}