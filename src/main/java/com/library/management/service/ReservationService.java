package com.library.management.service;

import com.library.management.domain.entity.Book;
import com.library.management.dto.response.ReservationResponse;

import java.util.List;

public interface ReservationService {

    ReservationResponse reserve(Long memberId, Long bookId);
    ReservationResponse cancel(Long reservationId, Long memberId);
    List<ReservationResponse> getMemberReservations(Long memberId);

    void fulfillReservation(Long memberId, Long bookId);

    void notifyNextInQueue(Book book);

    void expireNotifications();
}