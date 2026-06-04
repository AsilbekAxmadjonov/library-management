package com.library.management.controller;

import com.library.management.dto.response.FineResponse;
import com.library.management.service.FineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fines")
@RequiredArgsConstructor
@Tag(name = "Fines", description = "View and pay overdue fines")
public class FineController {

    private final FineService fineService;

    @GetMapping("/{id}")
    @Operation(summary = "Get fine by ID")
    public ResponseEntity<FineResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(fineService.getById(id));
    }

    @GetMapping("/member/{memberId}")
    @Operation(summary = "Get all fines for a member")
    public ResponseEntity<List<FineResponse>> getMemberFines(
            @PathVariable Long memberId) {
        return ResponseEntity.ok(fineService.getMemberFines(memberId));
    }

    @PatchMapping("/{id}/pay")
    @Operation(
            summary = "Pay a fine",
            description = "Marks fine as paid. If member was auto-blocked due to fines, re-activates them if total unpaid drops below threshold"
    )
    public ResponseEntity<FineResponse> pay(@PathVariable Long id) {
        return ResponseEntity.ok(fineService.payFine(id));
    }
}
