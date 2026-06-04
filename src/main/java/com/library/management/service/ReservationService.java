package com.library.management.service;

import com.library.management.dto.response.ReservationResponse;

import java.util.List;

// service/ReservationService.java
public interface ReservationService {

    ReservationResponse reserve(Long memberId, Long bookId);

    ReservationResponse cancel(Long reservationId, Long memberId);

    List<ReservationResponse> getMemberReservations(Long memberId);
}
