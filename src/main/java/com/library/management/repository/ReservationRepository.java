// repository/ReservationRepository.java
package com.library.management.repository;

import com.library.management.domain.entity.Reservation;
import com.library.management.domain.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // First WAITING member for a book — for queue ordering
    Optional<Reservation> findFirstByBookIdAndStatusOrderByReservedAtAsc(
            Long bookId, ReservationStatus status);

    // Check if member is already WAITING for a book
    boolean existsByMemberIdAndBookIdAndStatus(
            Long memberId, Long bookId, ReservationStatus status);

    // Check if any WAITING reservations exist for a book
    // Used in extendLoan to block extensions when queue exists
    boolean existsByBookIdAndStatus(
            Long bookId, ReservationStatus status);

    // All reservations for a member — for getMemberReservations()
    List<Reservation> findByMemberId(Long memberId);

    // Find specific NOTIFIED reservation for member + book
    // Used in fulfillReservation()
    Optional<Reservation> findByMemberIdAndBookIdAndStatus(
            Long memberId, Long bookId, ReservationStatus status);

    // Find all NOTIFIED reservations past their expiry date
    // Used by expireNotifications() scheduler
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.status = 'NOTIFIED'
        AND r.expiresAt < :today
    """)
    List<Reservation> findExpiredNotifications(@Param("today") LocalDate today);
}