package com.library.management.controller;

import com.library.management.dto.request.CreateMemberRequest;
import com.library.management.dto.response.BaseResponse;
import com.library.management.dto.response.MemberResponse;
import com.library.management.dto.response.PageResponse;
import com.library.management.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Tag(name = "Members", description = "Manage library members")
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    @Operation(summary = "Create a new member")
    public ResponseEntity<Void> create(@Valid @RequestBody CreateMemberRequest request) {
        MemberResponse created = memberService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/{member_id}")
    @Operation(summary = "Get member by ID")
    public BaseResponse<MemberResponse> getById(@PathVariable Long member_id) {
        return BaseResponse.success(memberService.getById(member_id));
    }

    @GetMapping
    @Operation(summary = "Get all members")
    public BaseResponse<PageResponse<MemberResponse>> getAll(
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return BaseResponse.success(memberService.getAll(pageable));
    }

    @PutMapping("/{member_id}")
    @Operation(summary = "Update member by ID")
    public BaseResponse<Void> update(
            @PathVariable Long member_id,
            @Valid @RequestBody CreateMemberRequest request) {
        memberService.update(member_id, request);
        return BaseResponse.success(null);
    }

    @DeleteMapping("/{member_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete member by ID")
    public void delete(@PathVariable Long member_id) {
        memberService.delete(member_id);
    }
}