package com.library.management.mapper;

import com.library.management.domain.entity.Fine;
import com.library.management.dto.response.FineResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface FineMapper {

    // Fine → FineResponse: flatten loan → member chain
    @Mapping(target = "loanId",       source = "loan.id")
    @Mapping(target = "memberId",     source = "loan.member.id")
    @Mapping(target = "memberFullName",
            expression = "java(fine.getLoan().getMember().getFirstName() + \" \" + fine.getLoan().getMember().getLastName())"
    )
    FineResponse toResponse(Fine fine);

    // No toEntity — Fine is always created by FineService logic, not from a request DTO
}
