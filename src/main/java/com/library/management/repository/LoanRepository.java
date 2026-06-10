package com.library.management.repository;

import com.library.management.domain.entity.Loan;
import com.library.management.domain.entity.Member;
import com.library.management.domain.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    long countByMemberIdAndStatus(Long memberId, LoanStatus status);

    List<Loan> findByMemberId(Long memberId);

    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.dueDate < :today")
    List<Loan> findOverdueLoans(@Param("today") LocalDate today);

    Optional<Loan> findByMemberIdAndBookIdAndStatus(Long memberId, Long bookId, LoanStatus status);

    @Query("SELECT l.member FROM Loan l WHERE l.status = 'OVERDUE' GROUP BY l.member")
    List<Member> findMembersWithOverdueLoans();
}
