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
                    "Book has available copies — borrow it directly",
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
        log.info("Reservation created: reservationId={} memberId={} bookId={}",
                saved.getId(), memberId, bookId);

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

        log.info("Reservation cancelled: id={} memberId={} previousStatus={}",
                reservationId, memberId, previousStatus);

        if (previousStatus == ReservationStatus.NOTIFIED) {
            Book book = bookRepository.findById(reservation.getBook().getId())
                    .orElseThrow(() -> BusinessException.notFound("Book", reservation.getBook().getId()));
            book.setAvailableCopies(book.getAvailableCopies() + 1);
            bookRepository.save(book);
            log.info("Available copies restored after NOTIFIED cancellation: " +
                            "bookId={} newAvailable={}",
                    book.getId(), book.getAvailableCopies());

            notifyNextInQueue(book);
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
                .findFirstByMemberIdAndBookIdAndStatus(
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
        Book freshBook = bookRepository.findById(book.getId())
                .orElseThrow(() -> BusinessException.notFound("Book", book.getId()));

        reservationRepository
                .findFirstByBookIdAndStatusOrderByReservedAtAsc(freshBook.getId())
                .ifPresent(next -> {
                    next.setStatus(ReservationStatus.NOTIFIED);
                    next.setExpiresAt(LocalDate.now().plusDays(3));
                    reservationRepository.save(next);

                    freshBook.setAvailableCopies(freshBook.getAvailableCopies() - 1);
                    freshBook.setReservedCopies(freshBook.getReservedCopies() + 1);
                    bookRepository.save(freshBook);

                    log.info("Next in queue notified: reservationId={} memberId={} " +
                                    "bookId={} expiresAt={} availableCopies={}",
                            next.getId(), next.getMember().getId(),
                            freshBook.getId(), next.getExpiresAt(),
                            freshBook.getAvailableCopies());
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

            log.info("Reservation expired: id={} memberId={} bookId={}",
                    reservation.getId(),
                    reservation.getMember().getId(),
                    reservation.getBook().getId());

            Book book = bookRepository.findById(reservation.getBook().getId())
                    .orElseThrow(() -> BusinessException.notFound("Book", reservation.getBook().getId()));
            book.setAvailableCopies(book.getAvailableCopies() + 1);
            bookRepository.save(book);
            log.info("Available copies restored after expiry: bookId={} newAvailable={}",
                    book.getId(), book.getAvailableCopies());

            notifyNextInQueue(book);
        }

        log.info("=== Reservation expiry done. Expired: {} ===", expired.size());
    }
}