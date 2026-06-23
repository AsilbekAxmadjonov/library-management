package com.library.management.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Getter
public class LibraryMetrics {

    private final MeterRegistry registry;

    private Counter loanIssuedCounter;
    private Counter loanReturnedCounter;
    private Counter loanExtendedCounter;
    private Counter fineCreatedCounter;
    private Counter finePaidCounter;
    private Counter reservationCreatedCounter;
    private Counter reservationCancelledCounter;
    private Counter reservationExpiredCounter;
    private Counter memberBlockedCounter;
    private Counter bookCreatedCounter;
    private Counter bookDeletedCounter;

    @PostConstruct
    public void init() {
        loanIssuedCounter = Counter.builder("library.loans.issued")
                .description("Total loans issued").register(registry);

        loanReturnedCounter = Counter.builder("library.loans.returned")
                .description("Total books returned").register(registry);

        loanExtendedCounter = Counter.builder("library.loans.extended")
                .description("Total loan extensions").register(registry);

        fineCreatedCounter = Counter.builder("library.fines.created")
                .description("Total fines created").register(registry);

        finePaidCounter = Counter.builder("library.fines.paid")
                .description("Total fines paid").register(registry);

        reservationCreatedCounter = Counter.builder("library.reservations.created")
                .description("Total reservations created").register(registry);

        reservationCancelledCounter = Counter.builder("library.reservations.cancelled")
                .description("Total reservations cancelled").register(registry);

        reservationExpiredCounter = Counter.builder("library.reservations.expired")
                .description("Total reservations expired (timeout)").register(registry);

        memberBlockedCounter = Counter.builder("library.members.blocked")
                .description("Total member auto-blocks due to fines").register(registry);

        bookCreatedCounter = Counter.builder("library.books.created")
                .description("Total books added to the library").register(registry);

        bookDeletedCounter = Counter.builder("library.books.deleted")
                .description("Total books deleted from the library").register(registry);
    }
}