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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// service/impl/ReservationServiceImpl.java
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

        if (member.getStatus() == MemberStatus.BLOCKED) {
            throw new BusinessException(ErrorCode.MEMBER_BLOCKED,
                    "Blocked members cannot reserve books", HttpStatus.FORBIDDEN);
        }
        if (book.getAvailableCopies() > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Book has available copies — borrow it directly instead of reserving",
                    HttpStatus.BAD_REQUEST);
        }
        boolean alreadyQueued = reservationRepository
                .existsByMemberIdAndBookIdAndStatus(memberId, bookId, ReservationStatus.WAITING);
        if (alreadyQueued) {
            throw new BusinessException(ErrorCode.ALREADY_RESERVED,
                    "You are already in the queue for this book", HttpStatus.CONFLICT);
        }

        Reservation reservation = new Reservation();
        reservation.setMember(member);
        reservation.setBook(book);
        reservation.setReservedAt(LocalDateTime.now());
        reservation.setStatus(ReservationStatus.WAITING);

        Reservation saved = reservationRepository.save(reservation);
        log.info("Reservation created: memberId={} bookId={}", memberId, bookId);
        return reservationMapper.toResponse(saved);
    }

    @Override
    public ReservationResponse cancel(Long reservationId, Long memberId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> BusinessException.notFound("Reservation", reservationId));

        if (!reservation.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "You can only cancel your own reservations", HttpStatus.FORBIDDEN);
        }
        if (reservation.getStatus() == ReservationStatus.FULFILLED
                || reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Reservation is already " + reservation.getStatus(), HttpStatus.CONFLICT);
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        log.info("Reservation cancelled: id={}", reservationId);
        return reservationMapper.toResponse(reservationRepository.save(reservation));
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
}
