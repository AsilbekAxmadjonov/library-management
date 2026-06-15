package com.library.management.service;

import com.library.management.config.LibraryProperties;
import com.library.management.config.LibraryProperties.MemberTypeConfig;
import com.library.management.domain.entity.Book;
import com.library.management.domain.entity.Fine;
import com.library.management.domain.entity.Loan;
import com.library.management.domain.entity.Member;
import com.library.management.domain.enums.FineStatus;
import com.library.management.domain.enums.LoanStatus;
import com.library.management.domain.enums.MemberStatus;
import com.library.management.domain.enums.MemberType;
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
import com.library.management.service.impl.LoanServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanService unit tests")
class LoanServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 10);
    private static final Clock FIXED_CLOCK =
            Clock.fixed(TODAY.atStartOfDay(ZoneId.of("Asia/Tashkent")).toInstant(),
                    ZoneId.of("Asia/Tashkent"));

    @Mock private LoanRepository       loanRepository;
    @Mock private BookRepository       bookRepository;
    @Mock private MemberRepository     memberRepository;
    @Mock private FineRepository       fineRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private ReservationService   reservationService;
    @Mock private LoanMapper           loanMapper;
    @Mock private LibraryProperties    props;

    @InjectMocks
    private LoanServiceImpl loanService;

    private Member member;
    private Book   book;

    @BeforeEach
    void setUp() {
        try {
            var field = LoanServiceImpl.class.getDeclaredField("clock");
            field.setAccessible(true);
            field.set(loanService, FIXED_CLOCK);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        member = new Member();
        member.setId(1L);
        member.setFirstName("Asilbek");
        member.setLastName("Aliyev");
        member.setStatus(MemberStatus.ACTIVE);
        member.setType(MemberType.STANDARD);

        book = new Book();
        book.setId(1L);
        book.setTitle("Clean Code");
        book.setAvailableCopies(3);
        book.setTotalCopies(3);
        book.setReservedCopies(0);
    }

    private LibraryProperties.MemberTypeConfig standardConfig() {
        return new MemberTypeConfig(500L, 5, 2, 0, 50_000L);
    }

    @Test
    @DisplayName("issueLoan — happy path: loan created, available copies decremented")
    void issueLoan_happyPath_createsLoan() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(props.configFor(MemberType.STANDARD)).thenReturn(standardConfig());
        when(loanRepository.countByMemberIdAndStatus(1L, LoanStatus.ACTIVE)).thenReturn(0L);
        when(fineRepository.sumUnpaidFinesByMemberId(1L)).thenReturn(0L);

        Loan savedLoan = new Loan();
        savedLoan.setId(10L);
        savedLoan.setMember(member);
        savedLoan.setBook(book);
        savedLoan.setLoanDate(TODAY);
        savedLoan.setDueDate(TODAY.plusDays(14));
        savedLoan.setStatus(LoanStatus.ACTIVE);
        when(loanRepository.save(any(Loan.class))).thenReturn(savedLoan);

        LoanResponse expectedResponse = new LoanResponse(
                10L, 1L, "Asilbek Aliyev", 1L, "Clean Code",
                TODAY, TODAY.plusDays(14), null, LoanStatus.ACTIVE, 0);
        when(loanMapper.toResponse(savedLoan)).thenReturn(expectedResponse);

        LoanResponse result = loanService.issueLoan(new IssueLoanRequest(1L, 1L));

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.status()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(result.dueDate()).isEqualTo(TODAY.plusDays(14));

        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(bookCaptor.capture());
        assertThat(bookCaptor.getValue().getAvailableCopies()).isEqualTo(2);
    }

    @Test
    @DisplayName("issueLoan — BLOCKED_BY_FINES member → MEMBER_BLOCKED exception")
    void issueLoan_blockedMember_throwsException() {
        member.setStatus(MemberStatus.BLOCKED_BY_FINES);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(props.configFor(MemberType.STANDARD)).thenReturn(standardConfig());

        assertThatThrownBy(() -> loanService.issueLoan(new IssueLoanRequest(1L, 1L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_BLOCKED));

        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("issueLoan — BLOCKED_MANUALLY member → MEMBER_BLOCKED exception")
    void issueLoan_manuallyBlockedMember_throwsException() {
        member.setStatus(MemberStatus.BLOCKED_MANUALLY);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(props.configFor(MemberType.STANDARD)).thenReturn(standardConfig());

        assertThatThrownBy(() -> loanService.issueLoan(new IssueLoanRequest(1L, 1L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_BLOCKED));
    }

    @Test
    @DisplayName("issueLoan — book with 0 available copies → NO_COPIES_AVAILABLE exception")
    void issueLoan_noCopies_throwsException() {
        book.setAvailableCopies(0);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(props.configFor(MemberType.STANDARD)).thenReturn(standardConfig());
        when(loanRepository.countByMemberIdAndStatus(1L, LoanStatus.ACTIVE)).thenReturn(0L);
        when(fineRepository.sumUnpaidFinesByMemberId(1L)).thenReturn(0L);

        assertThatThrownBy(() -> loanService.issueLoan(new IssueLoanRequest(1L, 1L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.NO_COPIES_AVAILABLE));
    }

    @Test
    @DisplayName("issueLoan — member already at max loans → LOAN_LIMIT_EXCEEDED exception")
    void issueLoan_loanLimitReached_throwsException() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(props.configFor(MemberType.STANDARD)).thenReturn(standardConfig());
        // 5 active loans = at the limit
        when(loanRepository.countByMemberIdAndStatus(1L, LoanStatus.ACTIVE)).thenReturn(5L);

        assertThatThrownBy(() -> loanService.issueLoan(new IssueLoanRequest(1L, 1L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.LOAN_LIMIT_EXCEEDED));
    }

    @Test
    @DisplayName("returnBook — overdue return creates a fine with correct amount")
    void returnBook_overdue_createsFine() {
        // loan was due 5 days ago → 5 overdue days × 500/day = 2500 tiyin
        LocalDate dueDate = TODAY.minusDays(5);

        Loan loan = buildActiveLoan(dueDate);
        when(loanRepository.findById(10L)).thenReturn(Optional.of(loan));
        when(bookRepository.save(any())).thenReturn(book);
        when(fineRepository.findLatestByLoanId(10L)).thenReturn(Optional.empty());
        when(props.configFor(MemberType.STANDARD)).thenReturn(standardConfig());
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);
        when(loanMapper.toResponse(any())).thenReturn(mock(LoanResponse.class));

        loanService.returnBook(10L);

        ArgumentCaptor<Fine> fineCaptor = ArgumentCaptor.forClass(Fine.class);
        verify(fineRepository).save(fineCaptor.capture());
        Fine savedFine = fineCaptor.getValue();
        assertThat(savedFine.getAmount()).isEqualTo(5 * 500L);   // 2500 tiyin
        assertThat(savedFine.getStatus()).isEqualTo(FineStatus.PENDING);
        assertThat(savedFine.getCalculatedUpTo()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("returnBook — on-time return: no fine created")
    void returnBook_onTime_noFine() {
        Loan loan = buildActiveLoan(TODAY.plusDays(1));
        when(loanRepository.findById(10L)).thenReturn(Optional.of(loan));
        when(bookRepository.save(any())).thenReturn(book);
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);
        when(loanMapper.toResponse(any())).thenReturn(mock(LoanResponse.class));

        loanService.returnBook(10L);

        verify(fineRepository, never()).save(any());
    }

    @Test
    @DisplayName("returnBook — price cap: fine capped at book price when price > 0")
    void returnBook_overdue_fineCappedAtBookPrice() {
        book.setPrice(1500L);
        LocalDate dueDate = TODAY.minusDays(10);

        Loan loan = buildActiveLoan(dueDate);
        when(loanRepository.findById(10L)).thenReturn(Optional.of(loan));
        when(bookRepository.save(any())).thenReturn(book);
        when(fineRepository.findLatestByLoanId(10L)).thenReturn(Optional.empty());
        when(props.configFor(MemberType.STANDARD)).thenReturn(standardConfig());
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);
        when(loanMapper.toResponse(any())).thenReturn(mock(LoanResponse.class));

        loanService.returnBook(10L);

        ArgumentCaptor<Fine> fineCaptor = ArgumentCaptor.forClass(Fine.class);
        verify(fineRepository).save(fineCaptor.capture());
        assertThat(fineCaptor.getValue().getAmount()).isEqualTo(1500L);
    }

    @Test
    @DisplayName("returnBook — price = 0: fine NOT capped at zero (M5 fix)")
    void returnBook_overdue_zeroPriceDoesNotZeroFine() {
        book.setPrice(0L);
        LocalDate dueDate = TODAY.minusDays(3);

        Loan loan = buildActiveLoan(dueDate);
        when(loanRepository.findById(10L)).thenReturn(Optional.of(loan));
        when(bookRepository.save(any())).thenReturn(book);
        when(fineRepository.findLatestByLoanId(10L)).thenReturn(Optional.empty());
        when(props.configFor(MemberType.STANDARD)).thenReturn(standardConfig());
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);
        when(loanMapper.toResponse(any())).thenReturn(mock(LoanResponse.class));

        loanService.returnBook(10L);

        ArgumentCaptor<Fine> fineCaptor = ArgumentCaptor.forClass(Fine.class);
        verify(fineRepository).save(fineCaptor.capture());
        assertThat(fineCaptor.getValue().getAmount()).isEqualTo(1500L);
    }

    @Test
    @DisplayName("returnBook — already returned → LOAN_ALREADY_RETURNED exception")
    void returnBook_alreadyReturned_throwsException() {
        Loan loan = buildActiveLoan(TODAY.plusDays(5));
        loan.setStatus(LoanStatus.RETURNED);
        when(loanRepository.findById(10L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.returnBook(10L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.LOAN_ALREADY_RETURNED));
    }

    @Test
    @DisplayName("extendLoan — overdue loan → EXTENSION_NOT_ALLOWED exception")
    void extendLoan_overdue_throwsException() {
        Loan loan = buildActiveLoan(TODAY.minusDays(1)); // past due
        when(loanRepository.findById(10L)).thenReturn(Optional.of(loan));
        when(props.configFor(MemberType.STANDARD)).thenReturn(standardConfig());

        assertThatThrownBy(() -> loanService.extendLoan(10L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.EXTENSION_NOT_ALLOWED));
    }

    @Test
    @DisplayName("extendLoan — max extensions reached → EXTENSION_NOT_ALLOWED exception")
    void extendLoan_maxExtensionsReached_throwsException() {
        Loan loan = buildActiveLoan(TODAY.plusDays(5));
        loan.setExtensionCount(2); // standard config allows 2 max
        when(loanRepository.findById(10L)).thenReturn(Optional.of(loan));
        when(props.configFor(MemberType.STANDARD)).thenReturn(standardConfig());

        assertThatThrownBy(() -> loanService.extendLoan(10L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.EXTENSION_NOT_ALLOWED));
    }

    @Test
    @DisplayName("extendLoan — happy path: due date extended, extensionCount incremented")
    void extendLoan_happyPath_extendsDueDate() {
        LocalDate originalDue = TODAY.plusDays(5);
        Loan loan = buildActiveLoan(originalDue);
        loan.setExtensionCount(0);

        LibraryProperties.Loan loanConfig = new LibraryProperties.Loan();
        loanConfig.setExtensionDays(7);

        when(loanRepository.findById(10L)).thenReturn(Optional.of(loan));
        when(props.configFor(MemberType.STANDARD)).thenReturn(standardConfig());
        when(props.getLoan()).thenReturn(loanConfig);
        when(reservationRepository.existsByBookIdAndStatus(any(), any())).thenReturn(false);
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);
        when(loanMapper.toResponse(any())).thenReturn(mock(LoanResponse.class));

        loanService.extendLoan(10L);

        assertThat(loan.getDueDate()).isEqualTo(originalDue.plusDays(7));
        assertThat(loan.getExtensionCount()).isEqualTo(1);
    }


    private Loan buildActiveLoan(LocalDate dueDate) {
        Loan loan = new Loan();
        loan.setId(10L);
        loan.setMember(member);
        loan.setBook(book);
        loan.setLoanDate(TODAY.minusDays(7));
        loan.setDueDate(dueDate);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setExtensionCount(0);
        return loan;
    }
}