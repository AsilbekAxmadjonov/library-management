package com.library.management.mapper;

import com.library.management.domain.entity.Fine;
import com.library.management.dto.response.FineResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface FineMapper {

    @Mapping(target = "loanId",       source = "loan.id")
    @Mapping(target = "memberId",     source = "loan.member.id")
    @Mapping(target = "memberFullName",
            expression = "java(fine.getLoan() != null && fine.getLoan().getMember() != null ? fine.getLoan().getMember().getFirstName() + \" \" + fine.getLoan().getMember().getLastName() : null)"    )
    FineResponse toResponse(Fine fine);

}
