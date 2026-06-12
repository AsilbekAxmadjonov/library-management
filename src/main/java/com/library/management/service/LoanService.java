package com.library.management.service;

import com.library.management.dto.request.IssueLoanRequest;
import com.library.management.dto.response.LoanResponse;

import java.util.List;

public interface LoanService {

    LoanResponse issueLoan(IssueLoanRequest request);

    LoanResponse returnBook(Long loanId);

    LoanResponse extendLoan(Long loanId);

    LoanResponse getById(Long id);

    List<LoanResponse> getMemberLoans(Long memberId);
}
