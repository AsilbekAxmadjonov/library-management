package com.library.management.repository;

import com.library.management.domain.entity.Fine;
import com.library.management.domain.enums.FineStatus;
import com.library.management.dto.projection.FineStatsProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FineRepository extends JpaRepository<Fine, Long> {

    @Query("SELECT f FROM Fine f WHERE f.loan.id = :loanId ORDER BY f.id DESC")
    List<Fine> findAllByLoanId(@Param("loanId") Long loanId);

    default Optional<Fine> findLatestByLoanId(Long loanId) {
        List<Fine> fines = findAllByLoanId(loanId);
        return fines.isEmpty() ? Optional.empty() : Optional.of(fines.get(0));
    }

    List<Fine> findByLoanMemberId(Long memberId);

    Page<Fine> findByStatus(FineStatus status, Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(f.amount), 0)
        FROM Fine f
        WHERE f.loan.member.id = :memberId
        AND f.status = 'PENDING'
    """)
    long sumUnpaidFinesByMemberId(@Param("memberId") Long memberId);

    @Query("""
        SELECT new com.library.management.dto.projection.FineStatsProjection(
            COUNT(f),
            COALESCE(SUM(f.amount), 0),
            COALESCE(SUM(CASE WHEN f.status = 'PAID' THEN f.amount ELSE 0 END), 0)
        )
        FROM Fine f
    """)
    FineStatsProjection getFineStats();

    long countByStatus(FineStatus status);
}