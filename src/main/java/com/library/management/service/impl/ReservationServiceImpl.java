// service/impl/ReservationServiceImpl.java
package com.library.management.service.impl;

import com.library.management.domain.entity.Book;
import com.library.management.domain.entity.Member;
import com.library.management.domain.entity.Reservation;
import com.library.management.domain.enums.MemberStatus;
import com.library.management.domain.enums.ReservationStatus;
import com.library.management.dto.response.ReservationResponse;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ErrorCode;
import com.library.management.mapper.ReservationMapper;
import com.library.management.repository.BookRepository;
import com.library.management.repository.MemberRepository;
import com.library.management.repository.ReservationRepository;
import com.library.management.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final ReservationMapper reservationMapper;

    // ── RESERVE ────────────────────────────────────────────────────
    @Override
    public ReservationResponse reserve(Long memberId, Long bookId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.notFound("Member", memberId));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> BusinessException.notFound("Book", bookId));

        // Rule 1: blocked members cannot reserve
        if (member.getStatus() == MemberStatus.BLOCKED) {
            throw new BusinessException(ErrorCode.MEMBER_BLOCKED,
                    "Blocked members cannot reserve books",
                    HttpStatus.FORBIDDEN);
        }

        // Rule 2: only reserve when no copies are available
        // If copies are available → member should borrow directly
        if (book.getAvailableCopies() > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Book '" + book.getTitle() +
                            "' has available copies — borrow it directly instead of reserving",
                    HttpStatus.BAD_REQUEST);
        }

        // Rule 3: member must not already be WAITING or NOTIFIED for this book
        // Prevents the same member from joining the queue twice
        boolean alreadyWaiting = reservationRepository
                .existsByMemberIdAndBookIdAndStatus(
                        memberId, bookId, ReservationStatus.WAITING);
        boolean alreadyNotified = reservationRepository
                .existsByMemberIdAndBookIdAndStatus(
                        memberId, bookId, ReservationStatus.NOTIFIED);

        if (alreadyWaiting || alreadyNotified) {
            throw new BusinessException(ErrorCode.ALREADY_RESERVED,
                    "You are already in the queue for: " + book.getTitle(),
                    HttpStatus.CONFLICT);
        }

        // All checks passed — create reservation
        Reservation reservation = new Reservation();
        reservation.setMember(member);
        reservation.setBook(book);
        reservation.setReservedAt(LocalDateTime.now());
        reservation.setStatus(ReservationStatus.WAITING);
        // expiresAt is null — only set when status becomes NOTIFIED

        Reservation saved = reservationRepository.save(reservation);

        log.info("Reservation created: reservationId={} memberId={} bookId={} memberType={}",
                saved.getId(), memberId, bookId, member.getType());

        return reservationMapper.toResponse(saved);
    }

    // ── CANCEL ─────────────────────────────────────────────────────
    @Override
    public ReservationResponse cancel(Long reservationId, Long memberId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> BusinessException.notFound("Reservation", reservationId));

        // Member can only cancel their own reservation
        if (!reservation.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "You can only cancel your own reservations",
                    HttpStatus.FORBIDDEN);
        }

        // Cannot cancel terminal states
        if (reservation.getStatus() == ReservationStatus.FULFILLED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Cannot cancel a fulfilled reservation",
                    HttpStatus.CONFLICT);
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Reservation is already cancelled",
                    HttpStatus.CONFLICT);
        }

        // Remember the old status before changing it
        // We need this to decide whether to notify next in queue
        ReservationStatus previousStatus = reservation.getStatus();

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        log.info("Reservation cancelled: reservationId={} memberId={} previousStatus={}",
                reservationId, memberId, previousStatus);

        // If a NOTIFIED member cancels → they had an active slot
        // The book is still available → notify the next WAITING member
        if (previousStatus == ReservationStatus.NOTIFIED) {
            notifyNextInQueue(reservation.getBook());
        }

        return reservationMapper.toResponse(reservation);
    }

    // ── GET MEMBER RESERVATIONS ────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getMemberReservations(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw BusinessException.notFound("Member", memberId);
        }
        return reservationRepository.findByMemberId(memberId)
                .stream()
                .map(reservationMapper::toResponse)
                .toList();
    }

    // ── FULFILL ────────────────────────────────────────────────────
    // Called by LoanServiceImpl.issueLoan()
    // When a notified member actually comes and borrows the book
    // their reservation must be closed as FULFILLED
    @Override
    public void fulfillReservation(Long memberId, Long bookId) {
        reservationRepository
                .findByMemberIdAndBookIdAndStatus(
                        memberId, bookId, ReservationStatus.NOTIFIED)
                .ifPresent(reservation -> {
                    reservation.setStatus(ReservationStatus.FULFILLED);
                    reservationRepository.save(reservation);
                    log.info("Reservation fulfilled: reservationId={} memberId={} bookId={}",
                            reservation.getId(), memberId, bookId);
                });
        // No NOTIFIED reservation found is fine —
        // member might be borrowing without having reserved first
    }

    // ── NOTIFY NEXT IN QUEUE ───────────────────────────────────────
    // Called by:
    //   1. LoanServiceImpl.returnBook()  → book just became available
    //   2. cancel()                      → NOTIFIED member gave up their slot
    //   3. expireNotifications()         → NOTIFIED member's deadline passed
    @Override
    public void notifyNextInQueue(Book book) {
        reservationRepository
                .findFirstByBookIdAndStatusOrderByReservedAtAsc(
                        book.getId(), ReservationStatus.WAITING)
                .ifPresent(next -> {
                    next.setStatus(ReservationStatus.NOTIFIED);
                    next.setExpiresAt(LocalDate.now().plusDays(3));
                    reservationRepository.save(next);
                    log.info("Next in queue notified: reservationId={} memberId={} " +
                                    "bookId={} expiresAt={}",
                            next.getId(), next.getMember().getId(),
                            book.getId(), next.getExpiresAt());
                });
    }

    // ── EXPIRE NOTIFICATIONS (Scheduler) ──────────────────────────
    // Runs daily at same time as fine update job
    // Finds NOTIFIED reservations whose 3-day deadline has passed
    // Cancels them and passes the slot to the next WAITING member
    @Override
    @Scheduled(cron = "${library.scheduler.fine-update-cron}")
    public void expireNotifications() {
        log.info("=== Reservation expiry check started ===");
        LocalDate today = LocalDate.now();

        List<Reservation> expired =
                reservationRepository.findExpiredNotifications(today);

        for (Reservation reservation : expired) {
            reservation.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(reservation);

            log.info("Reservation expired: reservationId={} memberId={} bookId={} " +
                            "expiredOn={}",
                    reservation.getId(),
                    reservation.getMember().getId(),
                    reservation.getBook().getId(),
                    reservation.getExpiresAt());

            // Pass the slot to the next person in line
            notifyNextInQueue(reservation.getBook());
        }

        log.info("=== Reservation expiry check done. Expired: {} ===",
                expired.size());
    }

    // ══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════

    private Reservation findById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Reservation", id));
    }
}