package com.library.management.service.impl;

import com.library.management.config.LibraryProperties;
import com.library.management.config.LibraryProperties.MemberTypeConfig;
import com.library.management.domain.entity.Fine;
import com.library.management.domain.entity.Loan;
import com.library.management.domain.entity.Member;
import com.library.management.domain.enums.FineStatus;
import com.library.management.domain.enums.LoanStatus;
import com.library.management.domain.enums.MemberStatus;
import com.library.management.dto.response.FineResponse;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ErrorCode;
import com.library.management.mapper.FineMapper;
import com.library.management.repository.FineRepository;
import com.library.management.repository.LoanRepository;
import com.library.management.repository.MemberRepository;
import com.library.management.service.FineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FineServiceImpl implements FineService {

    private final FineRepository fineRepository;
    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;
    private final FineMapper fineMapper;
    private final LibraryProperties props;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public FineResponse getById(Long id) {
        return fineMapper.toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FineResponse> getMemberFines(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw BusinessException.notFound("Member", memberId);
        }
        return fineRepository.findByLoanMemberId(memberId)
                .stream()
                .map(fineMapper::toResponse)
                .toList();
    }

    @Override
    public FineResponse payFine(Long fineId) {
        Fine fine = findById(fineId);

        if (fine.getStatus() == FineStatus.PAID) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Fine " + fineId + " is already paid",
                    HttpStatus.CONFLICT);
        }

        fine.setStatus(FineStatus.PAID);
        fine.setPaidAt(LocalDateTime.now(clock));
        fineRepository.save(fine);

        Member member = fine.getLoan().getMember();
        MemberTypeConfig config = props.configFor(member.getType());
        long remainingUnpaid = fineRepository.sumUnpaidFinesByMemberId(member.getId());

        if (member.getStatus() == MemberStatus.BLOCKED_BY_FINES
                && remainingUnpaid <= config.getMaxUnpaidThreshold()) {
            member.setStatus(MemberStatus.ACTIVE);
            memberRepository.save(member);
            log.info("Member auto-unblocked after payment: memberId={}", member.getId());
        }

        log.info("Fine paid: fineId={} memberId={}", fineId, member.getId());
        return fineMapper.toResponse(fine);
    }


    @Override
    @Scheduled(cron = "${library.scheduler.fine-update-cron}")
    public void runDailyFineUpdate() {
        log.info("=== Daily fine update job started ===");
        LocalDate today = LocalDate.now(clock);
        List<Loan> overdueLoans = loanRepository.findOverdueLoans(today);
        int updated = 0;

        for (Loan loan : overdueLoans) {
            Optional<Fine> existing = fineRepository.findLatestByLoanId(loan.getId());

            if (existing.isPresent()
                    && existing.get().getCalculatedUpTo().equals(today)) {
                continue;
            }

            long days = loan.overdueDays(today);
            MemberTypeConfig config = props.configFor(loan.getMember().getType());
            long billableDays = days - config.getGracePeriodDays();

            if (billableDays <= 0) {
                if (loan.getStatus() != LoanStatus.OVERDUE) {
                    loan.setStatus(LoanStatus.OVERDUE);
                    loanRepository.save(loan);
                }
                continue;
            }

            long amount = billableDays * config.getDailyRate();
            if (loan.getBook().getPrice() != null) {
                amount = Math.min(amount, loan.getBook().getPrice());
            }

            Fine fine = existing.orElse(new Fine());
            fine.setLoan(loan);
            fine.setAmount(amount);
            fine.setStatus(FineStatus.PENDING);
            fine.setCalculatedUpTo(today);
            fineRepository.save(fine);

            if (loan.getStatus() != LoanStatus.OVERDUE) {
                loan.setStatus(LoanStatus.OVERDUE);
                loanRepository.save(loan);
            }

            Member member = loan.getMember();
            long totalUnpaid = fineRepository
                    .sumUnpaidFinesByMemberId(member.getId());
            if (totalUnpaid > config.getMaxUnpaidThreshold()) {
                if (member.getStatus() == MemberStatus.ACTIVE) {
                    member.setStatus(MemberStatus.BLOCKED_BY_FINES);
                    memberRepository.save(member);
                    log.warn("Member auto-blocked: memberId={} type={} unpaidFines={}",
                            member.getId(), member.getType(), totalUnpaid);
                }
            }

            updated++;
        }

        log.info("=== Daily fine update completed. Updated: {} ===", updated);
    }

    private Fine findById(Long id) {
        return fineRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Fine", id));
    }
}