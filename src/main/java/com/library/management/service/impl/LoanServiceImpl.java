package com.library.management.service.impl;

import com.library.management.config.LibraryProperties;
import com.library.management.domain.entity.Book;
import com.library.management.domain.entity.Fine;
import com.library.management.domain.entity.Loan;
import com.library.management.domain.entity.Member;
import com.library.management.domain.enums.FineStatus;
import com.library.management.domain.enums.LoanStatus;
import com.library.management.domain.enums.MemberStatus;
import com.library.management.domain.enums.ReservationStatus;
import com.library.management.dto.request.IssueLoanRequest;
import com.library.management.dto.response.LoanResponse;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ErrorCode;
import com.library.management.mapper.LoanMapper;
import com.library.management.repository.*;
import com.library.management.service.LoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

// service/impl/LoanServiceImpl.java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final FineRepository fineRepository;
    private final ReservationRepository reservationRepository;
    private final LoanMapper loanMapper;
    private final LibraryProperties props;

    // ── ISSUE ──────────────────────────────────────────────────────
    @Override
    public LoanResponse issueLoan(IssueLoanRequest request) {
        Member member = findMember(request.memberId());
        Book book = findBook(request.bookId());

        validateMemberCanBorrow(member);
        validateBookAvailable(book);

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        Loan loan = new Loan();
        loan.setMember(member);
        loan.setBook(book);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(props.getLoan().getDefaultLoanDays()));
        loan.setStatus(LoanStatus.ACTIVE);

        Loan saved = loanRepository.save(loan);
        log.info("Loan issued: loanId={} memberId={} bookId={} due={}",
                saved.getId(), member.getId(), book.getId(), saved.getDueDate());
        return loanMapper.toResponse(saved);
    }

    // ── RETURN ─────────────────────────────────────────────────────
    @Override
    public LoanResponse returnBook(Long loanId) {
        Loan loan = findLoan(loanId);

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessException(ErrorCode.LOAN_ALREADY_RETURNED,
                    "Loan " + loanId + " is already returned", HttpStatus.CONFLICT);
        }

        loan.setReturnDate(LocalDate.now());
        loan.setStatus(LoanStatus.RETURNED);

        Book book = loan.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);

        if (loan.isOverdue()) {
            createOrUpdateFine(loan);
        }

        loanRepository.save(loan);
        notifyNextInQueue(book);

        log.info("Book returned: loanId={} overdue={}", loanId, loan.isOverdue());
        return loanMapper.toResponse(loan);
    }

    // ── EXTEND ─────────────────────────────────────────────────────
    @Override
    public LoanResponse extendLoan(Long loanId) {
        Loan loan = findLoan(loanId);

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessException(ErrorCode.EXTENSION_NOT_ALLOWED,
                    "Cannot extend a returned loan", HttpStatus.CONFLICT);
        }
        if (loan.isOverdue()) {
            throw new BusinessException(ErrorCode.EXTENSION_NOT_ALLOWED,
                    "Cannot extend an overdue loan — please return and pay the fine",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (loan.getExtensionCount() >= props.getLoan().getMaxExtensions()) {
            throw new BusinessException(ErrorCode.EXTENSION_NOT_ALLOWED,
                    "Maximum number of extensions (" + props.getLoan().getMaxExtensions() + ") reached",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        boolean hasQueue = reservationRepository
                .existsByBookIdAndStatus(loan.getBook().getId(), ReservationStatus.WAITING);
        if (hasQueue) {
            throw new BusinessException(ErrorCode.EXTENSION_NOT_ALLOWED,
                    "Cannot extend: other members are waiting for this book",
                    HttpStatus.CONFLICT);
        }

        loan.setDueDate(loan.getDueDate().plusDays(props.getLoan().getExtensionDays()));
        loan.setExtensionCount(loan.getExtensionCount() + 1);

        log.info("Loan extended: loanId={} newDueDate={} extensionCount={}",
                loanId, loan.getDueDate(), loan.getExtensionCount());
        return loanMapper.toResponse(loanRepository.save(loan));
    }

    @Override
    @Transactional(readOnly = true)
    public LoanResponse getById(Long id) {
        return loanMapper.toResponse(findLoan(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanResponse> getMemberLoans(Long memberId) {
        findMember(memberId); // validate member exists
        return loanRepository.findByMemberId(memberId)
                .stream()
                .map(loanMapper::toResponse)
                .toList();
    }

    // ── Validation helpers ─────────────────────────────────────────
    private void validateMemberCanBorrow(Member member) {
        if (member.getStatus() == MemberStatus.BLOCKED) {
            throw new BusinessException(ErrorCode.MEMBER_BLOCKED,
                    "Member is blocked and cannot borrow books", HttpStatus.FORBIDDEN);
        }
        long activeCount = loanRepository
                .countByMemberIdAndStatus(member.getId(), LoanStatus.ACTIVE);
        if (activeCount >= props.getLoan().getMaxBooksPerMember()) {
            throw new BusinessException(ErrorCode.LOAN_LIMIT_EXCEEDED,
                    "Loan limit of " + props.getLoan().getMaxBooksPerMember() + " reached",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        long unpaidFines = fineRepository.sumUnpaidFinesByMemberId(member.getId());
        if (unpaidFines > props.getFine().getMaxUnpaidThreshold()) {
            throw new BusinessException(ErrorCode.FINE_LIMIT_EXCEEDED,
                    "Unpaid fines exceed the allowed threshold — please pay your fines first",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void validateBookAvailable(Book book) {
        if (book.getAvailableCopies() <= 0) {
            throw new BusinessException(ErrorCode.NO_COPIES_AVAILABLE,
                    "No available copies for: " + book.getTitle(),
                    HttpStatus.CONFLICT);
        }
    }

    // ── Fine helper ────────────────────────────────────────────────
    private void createOrUpdateFine(Loan loan) {
        long overdueDays = loan.overdueDays();
        long amount = overdueDays * props.getFine().getDailyRate();

        // Bonus: cap fine at book price
        if (loan.getBook().getPrice() != null) {
            amount = Math.min(amount, loan.getBook().getPrice());
        }

        Fine fine = fineRepository.findByLoanId(loan.getId()).orElse(new Fine());
        fine.setLoan(loan);
        fine.setAmount(amount);
        fine.setStatus(FineStatus.PENDING);
        fine.setCalculatedUpTo(LocalDate.now());
        fineRepository.save(fine);

        log.info("Fine created/updated: loanId={} days={} amount={}",
                loan.getId(), overdueDays, amount);
    }

    // ── Reservation queue helper ───────────────────────────────────
    private void notifyNextInQueue(Book book) {
        reservationRepository
                .findFirstByBookIdAndStatusOrderByReservedAtAsc(book.getId(), ReservationStatus.WAITING)
                .ifPresent(reservation -> {
                    reservation.setStatus(ReservationStatus.NOTIFIED);
                    reservation.setExpiresAt(LocalDate.now().plusDays(3));
                    reservationRepository.save(reservation);
                    log.info("Reservation notified: memberId={} bookId={}",
                            reservation.getMember().getId(), book.getId());
                });
    }

    // ── Entity finders ─────────────────────────────────────────────
    private Loan findLoan(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Loan", id));
    }

    private Member findMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Member", id));
    }

    private Book findBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Book", id));
    }
}