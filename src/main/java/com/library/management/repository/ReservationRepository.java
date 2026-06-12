package com.library.management.repository;

import com.library.management.domain.entity.Reservation;
import com.library.management.domain.enums.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.book.id = :bookId
        AND r.status = 'WAITING'
        ORDER BY r.reservedAt ASC
        LIMIT 1
    """)
    List<Reservation> findTopByBookIdAndStatusOrderByReservedAtAsc(
            @Param("bookId") Long bookId);

    default Optional<Reservation> findFirstByBookIdAndStatusOrderByReservedAtAsc(Long bookId) {
        List<Reservation> results = findTopByBookIdAndStatusOrderByReservedAtAsc(bookId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    boolean existsByMemberIdAndBookIdAndStatus(
            Long memberId, Long bookId, ReservationStatus status);

    boolean existsByBookIdAndStatus(
            Long bookId, ReservationStatus status);

    List<Reservation> findByMemberId(Long memberId);

    Optional<Reservation> findFirstByMemberIdAndBookIdAndStatus(
            Long memberId, Long bookId, ReservationStatus status);

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.status = 'NOTIFIED'
        AND r.expiresAt < :today
    """)
    List<Reservation> findExpiredNotifications(@Param("today") LocalDate today);
}