package com.library.management.mapper;

import com.library.management.domain.entity.Member;
import com.library.management.dto.request.CreateMemberRequest;
import com.library.management.dto.response.MemberResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface MemberMapper {

    // Entity → Response DTO
    // All fields match by name: firstName, lastName, email, phone, status, type, createdAt
    MemberResponse toResponse(Member member);

    // Request DTO → Entity
    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "createdAt",    ignore = true)
    @Mapping(target = "updatedAt",    ignore = true)
    @Mapping(target = "status",       ignore = true)  // defaults to ACTIVE in entity
    @Mapping(target = "loans",        ignore = true)
//    @Mapping(target = "fines",        ignore = true)
    @Mapping(target = "reservations", ignore = true)
    Member toEntity(CreateMemberRequest request);

    // Partial update — status is NOT updated here (use block/activate endpoints instead)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "createdAt",    ignore = true)
    @Mapping(target = "updatedAt",    ignore = true)
    @Mapping(target = "status",       ignore = true)
    @Mapping(target = "loans",        ignore = true)
//    @Mapping(target = "fines",        ignore = true)
    @Mapping(target = "reservations", ignore = true)
    void updateEntity(CreateMemberRequest request, @MappingTarget Member member);
}