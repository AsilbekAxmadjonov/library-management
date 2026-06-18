package com.library.management.service;

import com.library.management.dto.request.CreateMemberRequest;
import com.library.management.dto.response.MemberResponse;
import com.library.management.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MemberService {

    MemberResponse create(CreateMemberRequest request);

    MemberResponse getById(Long id);

    PageResponse<MemberResponse> getAll(Pageable pageable);

    MemberResponse update(Long id, CreateMemberRequest request);

    MemberResponse block(Long id);

    MemberResponse activate(Long id);

    void delete(Long id);
}