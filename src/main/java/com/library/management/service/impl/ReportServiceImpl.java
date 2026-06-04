package com.library.management.service.impl;

import com.library.management.domain.entity.Book;
import com.library.management.dto.response.BookResponse;
import com.library.management.dto.response.FineStatsResponse;
import com.library.management.dto.response.MemberResponse;
import com.library.management.mapper.BookMapper;
import com.library.management.mapper.MemberMapper;
import com.library.management.repository.BookRepository;
import com.library.management.repository.FineRepository;
import com.library.management.repository.LoanRepository;
import com.library.management.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// service/impl/ReportServiceImpl.java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // all reads — set at class level
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final FineRepository fineRepository;
    private final BookMapper bookMapper;
    private final MemberMapper memberMapper;

    @Override
    public List<BookResponse> getMostReadBooks(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return bookRepository.findMostReadBooks(pageable)
                .stream()
                .map(row -> bookMapper.toResponse((Book) row[0]))
                .toList();
    }

    @Override
    public List<MemberResponse> getMembersWithOverdueLoans() {
        return loanRepository.findMembersWithOverdueLoans()
                .stream()
                .map(memberMapper::toResponse)
                .toList();
    }

    @Override
    public FineStatsResponse getFineStatistics() {
        Object[] stats = fineRepository.getFineStats();
        long total    = stats[0] != null ? ((Number) stats[0]).longValue() : 0L;
        long totalAmt = stats[1] != null ? ((Number) stats[1]).longValue() : 0L;
        long paidAmt  = stats[2] != null ? ((Number) stats[2]).longValue() : 0L;
        return new FineStatsResponse(total, totalAmt, paidAmt, totalAmt - paidAmt);
    }
}
