package com.library.management.controller;

import com.library.management.dto.response.ReservationResponse;
import com.library.management.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Queue for unavailable books")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @Operation(
            summary = "Reserve a book",
            description = "Only allowed when all copies are currently on loan. Member joins the queue in order."
    )
    public ResponseEntity<ReservationResponse> reserve(
            @RequestParam Long memberId,
            @RequestParam Long bookId) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reservationService.reserve(memberId, bookId));
    }

    @GetMapping("/member/{memberId}")
    @Operation(summary = "Get all reservations for a member")
    public ResponseEntity<List<ReservationResponse>> getMemberReservations(
            @PathVariable Long memberId) {
        return ResponseEntity.ok(
                reservationService.getMemberReservations(memberId));
    }

    @PatchMapping("/{reservation_id}/cancel")
    @Operation(summary = "Cancel a reservation")
    public ResponseEntity<ReservationResponse> cancel(
            @PathVariable Long reservation_id,
            @RequestParam Long memberId) {
        return ResponseEntity.ok(reservationService.cancel(reservation_id, memberId));
    }
}
