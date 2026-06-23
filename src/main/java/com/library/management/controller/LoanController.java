package com.library.management.controller;

import com.library.management.dto.request.IssueLoanRequest;
import com.library.management.dto.response.BaseResponse;
import com.library.management.dto.response.LoanResponse;
import com.library.management.dto.response.PageResponse;
import com.library.management.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "Issue, return and extend book loans")
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    @Operation(
            summary = "Issue a book to a member",
            description = "Validates member status, loan limit, fine threshold and book availability"
    )
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<LoanResponse> issue(@Valid @RequestBody IssueLoanRequest request) {
        return BaseResponse.success(loanService.issueLoan(request));
    }

    @GetMapping("/{loan_id}")
    @Operation(summary = "Get loan by ID")
    public BaseResponse<LoanResponse> getById(@PathVariable Long loan_id) {
        return BaseResponse.success(loanService.getById(loan_id));
    }

    @GetMapping("/member/{memberId}")
    @Operation(summary = "Get all loans for a member")
    public BaseResponse<PageResponse<LoanResponse>> getMemberLoans(
            @PathVariable Long memberId,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return BaseResponse.success(loanService.getMemberLoans(memberId, pageable));
    }

    @PatchMapping("/{loan_id}/return")
    @Operation(
            summary = "Return a book",
            description = "Closes the loan, restores book copy count, calculates fine if overdue, notifies next in reservation queue"
    )
    public BaseResponse<LoanResponse> returnBook(@PathVariable Long loan_id) {
        return BaseResponse.success(loanService.returnBook(loan_id));
    }

    @PatchMapping("/{loan_id}/extend")
    @Operation(
            summary = "Extend loan due date",
            description = "Not allowed if overdue, max extensions reached, or there is a reservation queue for this book"
    )
    public BaseResponse<LoanResponse> extend(@PathVariable Long loan_id) {
        return BaseResponse.success(loanService.extendLoan(loan_id));
    }

    @PostMapping("/issue-notified-member")
    @Operation(
            summary = "Issue a book to a notified member",
            description = "Validates member status, loan limit, fine threshold and book availability"
    )
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<LoanResponse> issueNotifiedMember(@Valid @RequestBody IssueLoanRequest request) {
        return BaseResponse.success(loanService.issueNotifiedMember(request));
    }
}