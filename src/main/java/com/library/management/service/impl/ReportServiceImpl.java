package com.library.management.service.impl;

import com.library.management.domain.entity.Book;
import com.library.management.dto.projection.FineStatsProjection;
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

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final FineRepository fineRepository;
    private final BookMapper bookMapper;
    private final MemberMapper memberMapper;
    private final Clock clock;

    @Override
    public List<BookResponse> getMostReadBooks(int limit) {
        log.debug("getMostReadBooks requested: limit={}", limit);
        return bookRepository.findMostReadBooks(PageRequest.of(0, limit))
                .stream()
                .map(row -> bookMapper.toResponse((com.library.management.domain.entity.Book) row[0]))
                .toList();
    }

    @Override
    public List<MemberResponse> getMembersWithOverdueLoans() {
        LocalDate today = LocalDate.now(clock);
        log.debug("getMembersWithOverdueLoans: date={}", today);
        return loanRepository.findMembersWithOverdueLoans(today)
                .stream()
                .map(memberMapper::toResponse)
                .toList();
    }

    @Override
    public FineStatsResponse getFineStatistics() {
        log.debug("getFineStatistics requested");

        FineStatsProjection stats = fineRepository.getFineStats();

        long total    = stats.totalFines()  != null ? stats.totalFines()  : 0L;
        long totalAmt = stats.totalAmount() != null ? stats.totalAmount() : 0L;
        long paidAmt  = stats.paidAmount()  != null ? stats.paidAmount()  : 0L;

        log.info("Fine statistics: totalFines={} totalAmount={} paidAmount={} unpaidAmount={}",
                total, totalAmt, paidAmt, totalAmt - paidAmt);

        return new FineStatsResponse(total, totalAmt, paidAmt, totalAmt - paidAmt);
    }
}
