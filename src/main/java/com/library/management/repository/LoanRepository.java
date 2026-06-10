package com.library.management.repository;

import com.library.management.domain.entity.Loan;
import com.library.management.domain.entity.Member;
import com.library.management.domain.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    long countByMemberIdAndStatus(Long memberId, LoanStatus status);

    List<Loan> findByMemberId(Long memberId);

    @Query("""
        SELECT l FROM Loan l
        WHERE l.status IN ('ACTIVE', 'OVERDUE')
        AND l.returnDate IS NULL
        AND l.dueDate < :today
    """)
    List<Loan> findOverdueLoans(@Param("today") LocalDate today);

    @Query("SELECT DISTINCT l.member FROM Loan l WHERE l.status = 'OVERDUE'")
    List<Member> findMembersWithOverdueLoans();
}