package com.library.management.mapper;

import com.library.management.domain.entity.Author;
import com.library.management.dto.request.CreateAuthorRequest;
import com.library.management.dto.response.AuthorResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AuthorMapper {

    // Entity → Response DTO
    AuthorResponse toResponse(Author author);

    // Request DTO → Entity
    // id, createdAt, updatedAt, books — ignored (managed by JPA/Hibernate)
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "books",     ignore = true)
    Author toEntity(CreateAuthorRequest request);

    // Partial update — only non-null fields from request are applied to existing entity
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "books",     ignore = true)
    void updateEntity(CreateAuthorRequest request, @MappingTarget Author author);
}
