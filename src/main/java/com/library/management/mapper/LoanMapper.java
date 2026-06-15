package com.library.management.mapper;

import com.library.management.domain.entity.Loan;
import com.library.management.dto.response.LoanResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface LoanMapper {

    @Mapping(target = "memberId",       source = "member.id")
    @Mapping(target = "memberFullName",
            expression = "java(loan.getMember() != null ? loan.getMember().getFirstName() + \" \" + loan.getMember().getLastName() : null)"
    )
    @Mapping(target = "bookId",         source = "book.id")
    @Mapping(target = "bookTitle",      source = "book.title")
    LoanResponse toResponse(Loan loan);

}
