// service/ReservationService.java
package com.library.management.service;

import com.library.management.domain.entity.Book;
import com.library.management.dto.response.ReservationResponse;

import java.util.List;

public interface ReservationService {

    // REST endpoints
    ReservationResponse reserve(Long memberId, Long bookId);
    ReservationResponse cancel(Long reservationId, Long memberId);
    List<ReservationResponse> getMemberReservations(Long memberId);

    // Called by LoanServiceImpl.issueLoan()
    void fulfillReservation(Long memberId, Long bookId);

    // Called by LoanServiceImpl.returnBook()
    void notifyNextInQueue(Book book);

    // Scheduler — runs daily
    void expireNotifications();
}