package com.library.management.mapper;

import com.library.management.domain.entity.Book;
import com.library.management.dto.request.CreateBookRequest;
import com.library.management.dto.response.BookResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BookMapper {

    // Entity → Response DTO
    // authorFullName is derived from author.firstName + author.lastName
    // We use a custom expression for this
    @Mapping(
            target = "authorFullName",
            expression = "java(book.getAuthor().getFirstName() + \" \" + book.getAuthor().getLastName())"
    )
    BookResponse toResponse(Book book);

    // Request DTO → Entity
    // author is set manually in service (needs Author entity fetched from DB)
    // availableCopies is set manually in service (= totalCopies on creation)
    @Mapping(target = "id",              ignore = true)
    @Mapping(target = "createdAt",       ignore = true)
    @Mapping(target = "updatedAt",       ignore = true)
    @Mapping(target = "author",          ignore = true)
    @Mapping(target = "availableCopies", ignore = true)
    @Mapping(target = "loans",           ignore = true)
    @Mapping(target = "reservations",    ignore = true)
    Book toEntity(CreateBookRequest request);

    // Partial update
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",              ignore = true)
    @Mapping(target = "createdAt",       ignore = true)
    @Mapping(target = "updatedAt",       ignore = true)
    @Mapping(target = "author",          ignore = true)
    @Mapping(target = "availableCopies", ignore = true)
    @Mapping(target = "loans",           ignore = true)
    @Mapping(target = "reservations",    ignore = true)
    void updateEntity(CreateBookRequest request, @MappingTarget Book book);
}
