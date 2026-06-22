package com.library.management.controller;

import com.library.management.domain.enums.FineStatus;
import com.library.management.dto.response.BaseResponse;
import com.library.management.dto.response.FineResponse;
import com.library.management.dto.response.PageResponse;
import com.library.management.service.FineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fines")
@RequiredArgsConstructor
@Tag(name = "Fines", description = "View and pay overdue fines")
public class FineController {

    private final FineService fineService;

    @PostMapping("/trigger-scheduler")
    @Operation(summary = "TESTING ONLY — manually trigger fine update job")
    public ResponseEntity<BaseResponse<String>> triggerScheduler() {
        fineService.runDailyFineUpdate();
        return ResponseEntity.ok(BaseResponse.success("Scheduler triggered successfully"));
    }

    @GetMapping("/{fine_id}")
    @Operation(summary = "Get fine by ID")
    public ResponseEntity<BaseResponse<FineResponse>> getById(@PathVariable Long fine_id) {
        return ResponseEntity.ok(BaseResponse.success(fineService.getById(fine_id)));
    }

    @GetMapping("/member/{memberId}")
    @Operation(summary = "Get all fines for a member")
    public ResponseEntity<BaseResponse<List<FineResponse>>> getMemberFines(
            @PathVariable Long memberId) {
        return ResponseEntity.ok(BaseResponse.success(fineService.getMemberFines(memberId)));
    }

    @PatchMapping("/{fine_id}/pay")
    @Operation(
            summary = "Pay a fine",
            description = "Marks fine as paid. If member was auto-blocked due to fines, re-activates them if total unpaid drops below threshold"
    )
    public ResponseEntity<BaseResponse<FineResponse>> pay(@PathVariable Long fine_id) {
        return ResponseEntity.ok(BaseResponse.success(fineService.payFine(fine_id)));
    }

    @GetMapping
    @Operation(
            summary = "Get all fines",
            description = "Optionally filter by status (PENDING or PAID)"
    )
    public ResponseEntity<BaseResponse<PageResponse<FineResponse>>> getAllFines(
            @RequestParam(required = false) FineStatus status,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(BaseResponse.success(fineService.getAllFines(status, pageable)));
    }
}