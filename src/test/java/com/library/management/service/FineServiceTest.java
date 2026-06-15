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
import com.library.management.dto.response.FineResponse;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ErrorCode;
import com.library.management.mapper.FineMapper;
import com.library.management.repository.FineRepository;
import com.library.management.repository.LoanRepository;
import com.library.management.repository.MemberRepository;
import com.library.management.service.impl.FineServiceImpl;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FineService unit tests")
class FineServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 10);
    private static final Clock FIXED_CLOCK =
            Clock.fixed(TODAY.atStartOfDay(ZoneId.of("Asia/Tashkent")).toInstant(),
                    ZoneId.of("Asia/Tashkent"));

    @Mock private FineRepository    fineRepository;
    @Mock private LoanRepository    loanRepository;
    @Mock private MemberRepository  memberRepository;
    @Mock private FineMapper        fineMapper;
    @Mock private LibraryProperties props;

    @InjectMocks
    private FineServiceImpl fineService;

    private Member member;
    private Book   book;

    @BeforeEach
    void setUp() {
        try {
            var field = FineServiceImpl.class.getDeclaredField("clock");
            field.setAccessible(true);
            field.set(fineService, FIXED_CLOCK);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        member = new Member();
        member.setId(1L);
        member.setType(MemberType.STANDARD);
        member.setStatus(MemberStatus.ACTIVE);

        book = new Book();
        book.setId(1L);
        book.setTitle("Clean Code");
        book.setPrice(null);
    }

    private MemberTypeConfig standardConfig() {
        return new MemberTypeConfig(500L, 5, 2, 0, 50_000L);
    }

    @Test
    @DisplayName("runDailyFineUpdate — idempotent: loan whose fine is already up to today is skipped")
    void updateOverdueFines_idempotent_doesNotDuplicate() {
        Loan loan = buildOverdueLoan(TODAY.minusDays(3));

        Fine existingFine = new Fine();
        existingFine.setId(5L);
        existingFine.setLoan(loan);
        existingFine.setCalculatedUpTo(TODAY); // already calculated today
        existingFine.setStatus(FineStatus.PENDING);

        when(loanRepository.findOverdueLoans(TODAY)).thenReturn(List.of(loan));
        when(fineRepository.findLatestByLoanId(loan.getId()))
                .thenReturn(Optional.of(existingFine));

        fineService.runDailyFineUpdate();

        verify(fineRepository, never()).save(any());
    }

    @Test
    @DisplayName("runDailyFineUpdate — new overdue loan: fine created with correct amount")
    void updateOverdueFines_newOverdueLoan_createsFine() {
        Loan loan = buildOverdueLoan(TODAY.minusDays(4));

        when(loanRepository.findOverdueLoans(TODAY)).thenReturn(List.of(loan));
        when(fineRepository.findLatestByLoanId(loan.getId())).thenReturn(Optional.empty());
        when(props.configFor(MemberType.STANDARD)).thenReturn(standardConfig());
        when(fineRepository.sumUnpaidFinesByMemberId(1L)).thenReturn(2000L);

        fineService.runDailyFineUpdate();

        ArgumentCaptor<Fine> captor = ArgumentCaptor.forClass(Fine.class);
        verify(fineRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualTo(4 * 500L);
        assertThat(captor.getValue().getCalculatedUpTo()).isEqualTo(TODAY);
        assertThat(captor.getValue().getStatus()).isEqualTo(FineStatus.PENDING);
    }

    @Test
    @DisplayName("runDailyFineUpdate — student grace period: no fine within grace window")
    void updateOverdueFines_withinGracePeriod_noFine() {
        member.setType(MemberType.STUDENT);
        Loan loan = buildOverdueLoan(TODAY.minusDays(1));

        MemberTypeConfig studentConfig = new MemberTypeConfig(250L, 3, 1, 2, 25_000L);

        when(loanRepository.findOverdueLoans(TODAY)).thenReturn(List.of(loan));
        when(fineRepository.findLatestByLoanId(loan.getId())).thenReturn(Optional.empty());
        when(props.configFor(MemberType.STUDENT)).thenReturn(studentConfig);

        fineService.runDailyFineUpdate();

        verify(fineRepository, never()).save(any());
        verify(loanRepository).save(loan);
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.OVERDUE);
    }

    @Test
    @DisplayName("runDailyFineUpdate — auto-blocks member when unpaid fines exceed threshold")
    void updateOverdueFines_exceedsThreshold_memberBlocked() {
        Loan loan = buildOverdueLoan(TODAY.minusDays(2));

        when(loanRepository.findOverdueLoans(TODAY)).thenReturn(List.of(loan));
        when(fineRepository.findLatestByLoanId(loan.getId())).thenReturn(Optional.empty());
        when(props.configFor(MemberType.STANDARD)).thenReturn(standardConfig());
        // total unpaid after this fine would be 60000 — above 50000 threshold
        when(fineRepository.sumUnpaidFinesByMemberId(1L)).thenReturn(60_000L);

        fineService.runDailyFineUpdate();

        verify(memberRepository).save(member);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.BLOCKED_BY_FINES);
    }

    @Test
    @DisplayName("payFine — happy path: status set to PAID, paidAt set")
    void payFine_marksAsPaid() {
        Fine fine = buildPendingFine(1000L);
        when(fineRepository.findById(5L)).thenReturn(Optional.of(fine));
        when(props.configFor(MemberType.STANDARD)).thenReturn(standardConfig());
        when(fineRepository.sumUnpaidFinesByMemberId(1L)).thenReturn(0L);
        when(fineMapper.toResponse(any())).thenReturn(mock(FineResponse.class));

        fineService.payFine(5L);

        assertThat(fine.getStatus()).isEqualTo(FineStatus.PAID);
        assertThat(fine.getPaidAt()).isNotNull();
        verify(fineRepository).save(fine);
    }

    @Test
    @DisplayName("payFine — already paid → INVALID_STATE_TRANSITION exception")
    void payFine_alreadyPaid_throwsException() {
        Fine fine = buildPendingFine(1000L);
        fine.setStatus(FineStatus.PAID);
        fine.setPaidAt(LocalDateTime.now());
        when(fineRepository.findById(5L)).thenReturn(Optional.of(fine));

        assertThatThrownBy(() -> fineService.payFine(5L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));

        verify(fineRepository, never()).save(any());
    }

    @Test
    @DisplayName("payFine — unpaid fines drop below threshold: member auto-unblocked")
    void payFine_unblocksMemberWhenThresholdMet() {
        member.setStatus(MemberStatus.BLOCKED_BY_FINES);
        Fine fine = buildPendingFine(20_000L);

        when(fineRepository.findById(5L)).thenReturn(Optional.of(fine));
        when(props.configFor(MemberType.STANDARD)).thenReturn(standardConfig());
        when(fineRepository.sumUnpaidFinesByMemberId(1L)).thenReturn(10_000L);
        when(fineMapper.toResponse(any())).thenReturn(mock(FineResponse.class));

        fineService.payFine(5L);

        verify(memberRepository).save(member);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    @DisplayName("payFine — unpaid fines still above threshold: member stays blocked")
    void payFine_remainsBlockedWhenThresholdNotMet() {
        member.setStatus(MemberStatus.BLOCKED_BY_FINES);
        Fine fine = buildPendingFine(5_000L);

        when(fineRepository.findById(5L)).thenReturn(Optional.of(fine));
        when(props.configFor(MemberType.STANDARD)).thenReturn(standardConfig());
        when(fineRepository.sumUnpaidFinesByMemberId(1L)).thenReturn(55_000L);
        when(fineMapper.toResponse(any())).thenReturn(mock(FineResponse.class));

        fineService.payFine(5L);

        verify(memberRepository, never()).save(any());
        assertThat(member.getStatus()).isEqualTo(MemberStatus.BLOCKED_BY_FINES);
    }

    @Test
    @DisplayName("payFine — fine not found → RESOURCE_NOT_FOUND exception")
    void payFine_notFound_throwsException() {
        when(fineRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fineService.payFine(99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private Loan buildOverdueLoan(LocalDate dueDate) {
        Loan loan = new Loan();
        loan.setId(10L);
        loan.setMember(member);
        loan.setBook(book);
        loan.setLoanDate(dueDate.minusDays(14));
        loan.setDueDate(dueDate);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setExtensionCount(0);
        return loan;
    }

    private Fine buildPendingFine(long amount) {
        Loan loan = new Loan();
        loan.setId(10L);
        loan.setMember(member);
        loan.setBook(book);

        Fine fine = new Fine();
        fine.setId(5L);
        fine.setLoan(loan);
        fine.setAmount(amount);
        fine.setStatus(FineStatus.PENDING);
        fine.setCalculatedUpTo(TODAY);
        return fine;
    }
}