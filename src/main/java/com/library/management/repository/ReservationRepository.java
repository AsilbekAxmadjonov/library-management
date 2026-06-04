package com.library.management.repository;

import com.library.management.domain.entity.Reservation;
import com.library.management.domain.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// ReservationRepository.java
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // First in queue for a book
    Optional<Reservation> findFirstByBookIdAndStatusOrderByReservedAtAsc(
            Long bookId, ReservationStatus status
    );

    boolean existsByMemberIdAndBookIdAndStatus(
            Long memberId, Long bookId, ReservationStatus status
    );

    boolean existsByBookIdAndStatus(Long bookId, ReservationStatus status);

    List<Reservation> findByMemberId(Long memberId);
}