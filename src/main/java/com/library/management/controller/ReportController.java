package com.library.management.controller;

import com.library.management.dto.response.BaseResponse;
import com.library.management.dto.response.BookResponse;
import com.library.management.dto.response.FineStatsResponse;
import com.library.management.dto.response.MemberResponse;
import com.library.management.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Read-only statistics and analytics")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/books/most-read")
    @Operation(summary = "Get most borrowed books")
    public BaseResponse<List<BookResponse>> getMostReadBooks(
            @Parameter(description = "How many top books to return")
            @RequestParam(defaultValue = "10") int limit) {
        return BaseResponse.success(reportService.getMostReadBooks(limit));
    }

    @GetMapping("/members/overdue")
    @Operation(summary = "Get members who currently have overdue loans")
    public BaseResponse<List<MemberResponse>> getMembersWithOverdueLoans() {
        return BaseResponse.success(reportService.getMembersWithOverdueLoans());
    }

    @GetMapping("/fines/stats")
    @Operation(summary = "Get fine statistics — total count, total amount, paid vs unpaid")
    public BaseResponse<FineStatsResponse> getFineStats() {
        return BaseResponse.success(reportService.getFineStatistics());
    }
}