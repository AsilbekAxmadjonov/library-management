package com.library.management.controller;

import com.library.management.dto.request.CreateMemberRequest;
import com.library.management.dto.response.MemberResponse;
import com.library.management.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    @Operation(summary = "Register a new member")
    public ResponseEntity<Void> create(@Valid @RequestBody CreateMemberRequest request) {
        MemberResponse created = memberService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get member by ID")
    public ResponseEntity<MemberResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.getById(id));
    }

    @GetMapping
    @Operation(summary = "Get all members")
    public ResponseEntity<List<MemberResponse>> getAll() {
        return ResponseEntity.ok(memberService.getAll());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update member info")
    public ResponseEntity<Void> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateMemberRequest request) {
        memberService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/block")
    @Operation(summary = "Block a member manually")
    public ResponseEntity<MemberResponse> block(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.block(id));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate a blocked member manually")
    public ResponseEntity<MemberResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.activate(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete member by ID")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        memberService.delete(id);
        return ResponseEntity.noContent().build();
    }
}