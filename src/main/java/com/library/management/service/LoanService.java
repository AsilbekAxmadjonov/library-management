package com.library.management.service;

import com.library.management.dto.request.IssueLoanRequest;
import com.library.management.dto.response.LoanResponse;
import com.library.management.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface LoanService {

    LoanResponse issueLoan(IssueLoanRequest request);

    LoanResponse returnBook(Long loanId);

    LoanResponse extendLoan(Long loanId);

    LoanResponse getById(Long id);

    PageResponse<LoanResponse> getMemberLoans(Long memberId, Pageable pageable);

    LoanResponse issueNotifiedMember(IssueLoanRequest issueLoanRequest);
}
