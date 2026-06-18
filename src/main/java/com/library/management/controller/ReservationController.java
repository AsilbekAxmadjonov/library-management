package com.library.management.controller;

import com.library.management.dto.response.BaseResponse;
import com.library.management.dto.response.PageResponse;
import com.library.management.dto.response.ReservationResponse;
import com.library.management.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<ReservationResponse> reserve(
            @RequestParam Long memberId,
            @RequestParam Long bookId) {
        return BaseResponse.success(reservationService.reserve(memberId, bookId));
    }

    @GetMapping("/member/{memberId}")
    @Operation(summary = "Get all reservations for a member")
    public BaseResponse<PageResponse<ReservationResponse>> getMemberReservations(
            @PathVariable Long memberId,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return BaseResponse.success(reservationService.getMemberReservations(memberId, pageable));
    }

    @PatchMapping("/{reservation_id}/cancel")
    @Operation(summary = "Cancel a reservation")
    public BaseResponse<ReservationResponse> cancel(
            @PathVariable Long reservation_id,
            @RequestParam Long memberId) {
        return BaseResponse.success(reservationService.cancel(reservation_id, memberId));
    }
}