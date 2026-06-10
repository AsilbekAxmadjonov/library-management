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

    @Override
    public ReservationResponse reserve(Long memberId, Long bookId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.notFound("Member", memberId));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> BusinessException.notFound("Book", bookId));

        if (member.getStatus() == MemberStatus.BLOCKED_BY_FINES
                || member.getStatus() == MemberStatus.BLOCKED_MANUALLY) {
            throw new BusinessException(ErrorCode.MEMBER_BLOCKED,
                    "Blocked members cannot reserve books",
                    HttpStatus.FORBIDDEN);
        }

        if (book.getAvailableCopies() > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Book '" + book.getTitle() +
                            "' has available copies — borrow it directly instead of reserving",
                    HttpStatus.BAD_REQUEST);
        }

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

        Reservation reservation = new Reservation();
        reservation.setMember(member);
        reservation.setBook(book);
        reservation.setReservedAt(LocalDateTime.now());
        reservation.setStatus(ReservationStatus.WAITING);

        Reservation saved = reservationRepository.save(reservation);

        log.info("Reservation created: reservationId={} memberId={} bookId={} memberType={}",
                saved.getId(), memberId, bookId, member.getType());

        return reservationMapper.toResponse(saved);
    }

    @Override
    public ReservationResponse cancel(Long reservationId, Long memberId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> BusinessException.notFound("Reservation", reservationId));

        if (!reservation.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "You can only cancel your own reservations",
                    HttpStatus.FORBIDDEN);
        }

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

        ReservationStatus previousStatus = reservation.getStatus();

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        log.info("Reservation cancelled: reservationId={} memberId={} previousStatus={}",
                reservationId, memberId, previousStatus);

        if (previousStatus == ReservationStatus.NOTIFIED) {
            notifyNextInQueue(reservation.getBook());
        }

        return reservationMapper.toResponse(reservation);
    }

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
    }

    @Override
    public void notifyNextInQueue(Book book) {
        reservationRepository
                .findFirstByBookIdAndStatusOrderByReservedAtAsc(book.getId())
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

            notifyNextInQueue(reservation.getBook());
        }

        log.info("=== Reservation expiry check done. Expired: {} ===",
                expired.size());
    }
}