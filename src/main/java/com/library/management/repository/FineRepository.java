package com.library.management.repository;

import com.library.management.domain.entity.Fine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// FineRepository.java
public interface FineRepository extends JpaRepository<Fine, Long> {

    Optional<Fine> findByLoanId(Long loanId);

    // Sum of unpaid fines for a member
    @Query("""
        SELECT COALESCE(SUM(f.amount), 0) 
        FROM Fine f 
        WHERE f.loan.member.id = :memberId AND f.status = 'PENDING'
    """)
    long sumUnpaidFinesByMemberId(@Param("memberId") Long memberId);

    // For reports
    @Query("SELECT COUNT(f), SUM(f.amount), SUM(CASE WHEN f.status='PAID' THEN f.amount ELSE 0 END) FROM Fine f")
    Object[] getFineStats();
}
