package com.library.management.service;

import com.library.management.dto.request.CreateMemberRequest;
import com.library.management.dto.response.MemberResponse;

import java.util.List;

// service/MemberService.java
public interface MemberService {

    MemberResponse create(CreateMemberRequest request);

    MemberResponse getById(Long id);

    List<MemberResponse> getAll();

    MemberResponse update(Long id, CreateMemberRequest request);

    MemberResponse block(Long id);

    MemberResponse activate(Long id);

    void delete(Long id);
}