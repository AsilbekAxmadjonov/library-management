package com.library.management.mapper;

import com.library.management.domain.entity.Reservation;
import com.library.management.dto.response.ReservationResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    @Mapping(target = "memberId",       source = "member.id")
    @Mapping(target = "memberFullName",
            expression = "java(reservation.getMember().getFirstName() + \" \" + reservation.getMember().getLastName())"
    )
    @Mapping(target = "bookId",         source = "book.id")
    @Mapping(target = "bookTitle",      source = "book.title")
    ReservationResponse toResponse(Reservation reservation);
}