package com.library.management.controller;

import com.library.management.dto.request.IssueLoanRequest;
import com.library.management.dto.response.LoanResponse;
import com.library.management.service.LoanService;
import com.library.management.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<LoanResponse> issue(
            @Valid @RequestBody IssueLoanRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(loanService.issueLoan(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loan by ID")
    public ResponseEntity<LoanResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.getById(id));
    }

    @GetMapping("/member/{memberId}")
    @Operation(summary = "Get all loans for a member")
    public ResponseEntity<List<LoanResponse>> getMemberLoans(
            @PathVariable Long memberId) {
        return ResponseEntity.ok(loanService.getMemberLoans(memberId));
    }

    @PatchMapping("/{id}/return")
    @Operation(
            summary = "Return a book",
            description = "Closes the loan, restores book copy count, calculates fine if overdue, notifies next in reservation queue"
    )
    public ResponseEntity<LoanResponse> returnBook(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.returnBook(id));
    }

    @PatchMapping("/{id}/extend")
    @Operation(
            summary = "Extend loan due date",
            description = "Not allowed if overdue, max extensions reached, or there is a reservation queue for this book"
    )
    public ResponseEntity<LoanResponse> extend(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.extendLoan(id));
    }
}

