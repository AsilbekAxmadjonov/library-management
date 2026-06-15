package com.library.management.mapper;

import com.library.management.domain.entity.Book;
import com.library.management.dto.request.CreateBookRequest;
import com.library.management.dto.response.BookResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BookMapper {


    @Mapping(
            target = "authorFullName",
            expression = "java(book.getAuthor() != null ? book.getAuthor().getFirstName() + \" \" + book.getAuthor().getLastName() : null)"
    )
    BookResponse toResponse(Book book);

    @Mapping(target = "id",              ignore = true)
    @Mapping(target = "createdAt",       ignore = true)
    @Mapping(target = "updatedAt",       ignore = true)
    @Mapping(target = "author",          ignore = true)
    @Mapping(target = "availableCopies", ignore = true)
    @Mapping(target = "loans",           ignore = true)
    @Mapping(target = "reservations",    ignore = true)
    Book toEntity(CreateBookRequest request);

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
