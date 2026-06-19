package com.library.management.repository;

import com.library.management.domain.entity.Loan;
import com.library.management.domain.entity.Member;
import com.library.management.domain.enums.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    long countByMemberIdAndStatus(Long memberId, LoanStatus status);

    Page<Loan> findByMemberId(Long memberId, Pageable pageable);

    @Query("""
        SELECT l FROM Loan l
        WHERE l.status IN ('ACTIVE', 'OVERDUE')
        AND l.returnDate IS NULL
        AND l.dueDate < :today
    """)
    List<Loan> findOverdueLoans(@Param("today") LocalDate today);

    @Query("""
        SELECT DISTINCT l.member FROM Loan l
        WHERE l.returnDate IS NULL
        AND l.dueDate < :today
        AND l.status IN ('ACTIVE', 'OVERDUE')
    """)
    List<Member> findMembersWithOverdueLoans(@Param("today") LocalDate today);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.status = :status")
    long countByStatus(@Param("status") LoanStatus status);
}