package com.library.management.service;

import com.library.management.dto.response.BookResponse;
import com.library.management.dto.response.FineStatsResponse;
import com.library.management.dto.response.MemberResponse;

import java.util.List;

public interface ReportService {

    List<BookResponse> getMostReadBooks(int limit);

    List<MemberResponse> getMembersWithOverdueLoans();

    FineStatsResponse getFineStatistics();
}
