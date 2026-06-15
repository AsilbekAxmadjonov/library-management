package com.library.management.service.impl;

import com.library.management.config.LibraryProperties;
import com.library.management.config.LibraryProperties.MemberTypeConfig;
import com.library.management.domain.entity.*;
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

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    private final Clock clock;

    @Override
    public LoanResponse issueLoan(IssueLoanRequest request) {
        Member member = findMember(request.memberId());
        Book book = findBook(request.bookId());

        validateMemberCanBorrow(member);
        validateBookAvailable(book);

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        LocalDate today = LocalDate.now(clock);

        Loan loan = new Loan();
        loan.setMember(member);
        loan.setBook(book);
        loan.setLoanDate(today);
        loan.setDueDate(today.plusDays(props.getLoan().getDefaultLoanDays()));
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

        LocalDate today = LocalDate.now(clock);

        loan.setReturnDate(today);
        loan.setStatus(LoanStatus.RETURNED);

        Book book = loan.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);

        if (loan.isOverdue(today)) {
            createOrUpdateFine(loan, today);
        }

        loanRepository.save(loan);
        reservationService.notifyNextInQueue(book);

        log.info("Book returned: loanId={} memberId={} memberType={} overdue={}",
                loanId, loan.getMember().getId(),
                loan.getMember().getType(), loan.isOverdue(today));

        return loanMapper.toResponse(loan);
    }

    @Override
    public LoanResponse extendLoan(Long loanId) {
        Loan loan = findLoan(loanId);
        Member member = loan.getMember();
        MemberTypeConfig config = props.configFor(member.getType());

        LocalDate today = LocalDate.now(clock);

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessException(ErrorCode.EXTENSION_NOT_ALLOWED,
                    "Cannot extend a returned loan", HttpStatus.CONFLICT);
        }
        if (loan.isOverdue(today)) {
            throw new BusinessException(ErrorCode.EXTENSION_NOT_ALLOWED,
                    "Cannot extend an overdue loan",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (loan.getExtensionCount() >= config.getMaxExtensions()) {
            throw new BusinessException(ErrorCode.EXTENSION_NOT_ALLOWED,
                    "Maximum extensions (" + config.getMaxExtensions() +
                            ") reached for member type: " + member.getType(),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        boolean hasQueue =
                reservationRepository.existsByBookIdAndStatus(
                        loan.getBook().getId(), ReservationStatus.WAITING)
                        || reservationRepository.existsByBookIdAndStatus(
                        loan.getBook().getId(), ReservationStatus.NOTIFIED);

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
        findMember(memberId);
        return loanRepository.findByMemberId(memberId)
                .stream()
                .map(loanMapper::toResponse)
                .toList();
    }

    @Override
    public LoanResponse issueNotifiedMember(IssueLoanRequest request) {
        Member member = findMember(request.memberId());
        Book book = findBook(request.bookId());

        Optional<Reservation> reservation = reservationRepository.findReservation(member.getId(), book.getId(), ReservationStatus.NOTIFIED.name());

        if (reservation.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Reservation not found",
                    HttpStatus.NOT_FOUND);
        }

        validateMemberCanBorrow(member);
        validateBookReserved(book);

        book.setReservedCopies(book.getReservedCopies() - 1);
        bookRepository.save(book);

        LocalDate today = LocalDate.now(clock);

        Loan loan = new Loan();
        loan.setMember(member);
        loan.setBook(book);
        loan.setLoanDate(today);
        loan.setDueDate(today.plusDays(props.getLoan().getDefaultLoanDays()));
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setExtensionCount(0);

        Loan saved = loanRepository.save(loan);
        reservationService.fulfillReservation(member.getId(), book.getId());

        log.info("Loan issued: loanId={} memberId={} bookId={} memberType={} due={}",
                saved.getId(), member.getId(), book.getId(),
                member.getType(), saved.getDueDate());

        return loanMapper.toResponse(saved);
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

    private void validateBookReserved(Book book) {
        if (book.getReservedCopies() <= 0) {
            throw new BusinessException(ErrorCode.NO_COPIES_AVAILABLE,
                    "No available copies for: " + book.getTitle(),
                    HttpStatus.CONFLICT);
        }
    }

    private void createOrUpdateFine(Loan loan, LocalDate today) {
        MemberTypeConfig config = props.configFor(loan.getMember().getType());

        long overdueDays = loan.overdueDays(today);
        long billableDays = overdueDays - config.getGracePeriodDays();

        if (billableDays <= 0) {
            log.info("Loan {} within grace period — no fine. memberType={}",
                    loan.getId(), loan.getMember().getType());
            return;
        }

        long amount = billableDays * config.getDailyRate();

        Long bookPrice = loan.getBook().getPrice();
        if (bookPrice != null && bookPrice > 0) {
            amount = Math.min(amount, bookPrice);
        }

        Optional<Fine> existingOpt = fineRepository.findLatestByLoanId(loan.getId());

        if (existingOpt.isPresent()) {
            Fine existing = existingOpt.get();

            if (existing.getStatus() == FineStatus.PAID) {
                log.info("Fine for loanId={} already paid — skipping fine creation on return",
                        loan.getId());
                return;
            }

            if (existing.getCalculatedUpTo().equals(today)) {
                log.info("Fine already calculated today for loanId={} — skipping",
                        loan.getId());
                return;
            }
        }

        Fine fine = existingOpt.orElse(new Fine());
        fine.setLoan(loan);
        fine.setAmount(amount);
        fine.setStatus(FineStatus.PENDING);
        fine.setCalculatedUpTo(today);
        fineRepository.save(fine);

        log.info("Fine saved on return: loanId={} memberType={} billableDays={} amount={}",
                loan.getId(), loan.getMember().getType(), billableDays, amount);
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