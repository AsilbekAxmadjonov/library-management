package com.library.management.service.impl;

import com.library.management.config.LibraryProperties;
import com.library.management.config.LibraryProperties.MemberTypeConfig;
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
import com.library.management.repository.BookRepository;
import com.library.management.repository.FineRepository;
import com.library.management.repository.LoanRepository;
import com.library.management.repository.MemberRepository;
import com.library.management.repository.ReservationRepository;
import com.library.management.service.LoanService;
import com.library.management.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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
    private final ReservationService reservationService;
    private final LoanMapper loanMapper;
    private final LibraryProperties props;

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
        loan.setDueDate(LocalDate.now()
                .plusDays(props.getLoan().getDefaultLoanDays()));
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setExtensionCount(0);

        Loan saved = loanRepository.save(loan);

        reservationService.fulfillReservation(member.getId(), book.getId());

        log.info("Loan issued: loanId={} memberId={} bookId={} memberType={} due={}",
                saved.getId(), member.getId(), book.getId(),
                member.getType(), saved.getDueDate());

        return loanMapper.toResponse(saved);
    }

    @Override
    public LoanResponse returnBook(Long loanId) {
        Loan loan = findLoan(loanId);

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessException(ErrorCode.LOAN_ALREADY_RETURNED,
                    "Loan " + loanId + " is already returned",
                    HttpStatus.CONFLICT);
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

        reservationService.notifyNextInQueue(book);

        log.info("Book returned: loanId={} memberId={} memberType={} overdue={}",
                loanId, loan.getMember().getId(),
                loan.getMember().getType(), loan.isOverdue());

        return loanMapper.toResponse(loan);
    }

    @Override
    public LoanResponse extendLoan(Long loanId) {
        Loan loan = findLoan(loanId);
        Member member = loan.getMember();

        MemberTypeConfig config = props.configFor(member.getType());

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessException(ErrorCode.EXTENSION_NOT_ALLOWED,
                    "Cannot extend a returned loan",
                    HttpStatus.CONFLICT);
        }

        if (loan.isOverdue()) {
            throw new BusinessException(ErrorCode.EXTENSION_NOT_ALLOWED,
                    "Cannot extend an overdue loan — please return and pay the fine",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Cannot exceed member-type-specific max extensions
        if (loan.getExtensionCount() >= config.getMaxExtensions()) {
            throw new BusinessException(ErrorCode.EXTENSION_NOT_ALLOWED,
                    "Maximum extensions (" + config.getMaxExtensions() +
                            ") reached for member type: " + member.getType(),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        boolean hasQueue = reservationRepository
                .existsByBookIdAndStatus(
                        loan.getBook().getId(), ReservationStatus.WAITING);
        if (hasQueue) {
            throw new BusinessException(ErrorCode.EXTENSION_NOT_ALLOWED,
                    "Cannot extend: other members are waiting for this book",
                    HttpStatus.CONFLICT);
        }

        loan.setDueDate(loan.getDueDate()
                .plusDays(props.getLoan().getExtensionDays()));
        loan.setExtensionCount(loan.getExtensionCount() + 1);

        log.info("Loan extended: loanId={} memberId={} memberType={} " +
                        "newDueDate={} extensionCount={}",
                loanId, member.getId(), member.getType(),
                loan.getDueDate(), loan.getExtensionCount());

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
        findMember(memberId); // validates member exists → 404 if not
        return loanRepository.findByMemberId(memberId)
                .stream()
                .map(loanMapper::toResponse)
                .toList();
    }

    private void validateMemberCanBorrow(Member member) {

        if (member.getStatus() == MemberStatus.BLOCKED_BY_FINES
                || member.getStatus() == MemberStatus.BLOCKED_MANUALLY) {
            throw new BusinessException(ErrorCode.MEMBER_BLOCKED,
                    "Member is blocked and cannot borrow books",
                    HttpStatus.FORBIDDEN);
        }

        MemberTypeConfig config = props.configFor(member.getType());

        long activeLoans = loanRepository
                .countByMemberIdAndStatus(member.getId(), LoanStatus.ACTIVE);
        if (activeLoans >= config.getMaxBooks()) {
            throw new BusinessException(ErrorCode.LOAN_LIMIT_EXCEEDED,
                    "Loan limit of " + config.getMaxBooks() +
                            " reached for member type: " + member.getType(),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        long unpaidFines = fineRepository.sumUnpaidFinesByMemberId(member.getId());
        if (unpaidFines > config.getMaxUnpaidThreshold()) {
            throw new BusinessException(ErrorCode.FINE_LIMIT_EXCEEDED,
                    "Unpaid fines exceed the threshold for member type: " + member.getType(),
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

    private void createOrUpdateFine(Loan loan) {
        MemberTypeConfig config = props.configFor(loan.getMember().getType());

        long overdueDays  = loan.overdueDays();

        // Subtract grace period:
        // STANDARD = 0 free days
        // STUDENT  = 2 free days  ← fine only starts from day 3
        // PREMIUM  = 1 free day   ← fine only starts from day 2
        long billableDays = overdueDays - config.getGracePeriodDays();

        // Still within grace period — no fine
        if (billableDays <= 0) {
            log.info("Loan {} returned within grace period ({} days) — no fine. " +
                            "memberType={}",
                    loan.getId(), config.getGracePeriodDays(),
                    loan.getMember().getType());
            return;
        }

        // billableDays × dailyRate for this member type
        long amount = billableDays * config.getDailyRate();

        // Cap fine at book price if the book has a price set
        if (loan.getBook().getPrice() != null) {
            amount = Math.min(amount, loan.getBook().getPrice());
        }

        // Create new fine or update existing one (idempotent)
        Fine fine = fineRepository
                .findByLoanId(loan.getId())
                .orElse(new Fine());

        fine.setLoan(loan);
        fine.setAmount(amount);
        fine.setStatus(FineStatus.PENDING);
        fine.setCalculatedUpTo(LocalDate.now());
        fineRepository.save(fine);

        log.info("Fine created/updated on return: loanId={} memberType={} " +
                        "overdueDays={} gracePeriod={} billableDays={} " +
                        "dailyRate={} amount={}",
                loan.getId(), loan.getMember().getType(),
                overdueDays, config.getGracePeriodDays(),
                billableDays, config.getDailyRate(), amount);
    }

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