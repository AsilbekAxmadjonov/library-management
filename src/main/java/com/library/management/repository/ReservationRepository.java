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

    Optional<Reservation> findFirstByBookIdAndStatusOrderByReservedAtAsc(
            Long bookId, ReservationStatus status);

    boolean existsByMemberIdAndBookIdAndStatus(
            Long memberId, Long bookId, ReservationStatus status);

    boolean existsByBookIdAndStatus(
            Long bookId, ReservationStatus status);

    List<Reservation> findByMemberId(Long memberId);


    Optional<Reservation> findByMemberIdAndBookIdAndStatus(
            Long memberId, Long bookId, ReservationStatus status);

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.status = 'NOTIFIED'
        AND r.expiresAt < :today
    """)
    List<Reservation> findExpiredNotifications(@Param("today") LocalDate today);
}