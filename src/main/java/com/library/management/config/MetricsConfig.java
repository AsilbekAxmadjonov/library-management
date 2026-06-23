package com.library.management.config;

import com.library.management.domain.enums.FineStatus;
import com.library.management.domain.enums.LoanStatus;
import com.library.management.domain.enums.ReservationStatus;
import com.library.management.repository.FineRepository;
import com.library.management.repository.LoanRepository;
import com.library.management.repository.ReservationRepository;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    private final LoanRepository loanRepository;
    private final FineRepository fineRepository;
    private final ReservationRepository reservationRepository;

    // Enables @Timed annotation on service methods
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    // Gauges — polled on every Prometheus scrape (always current)
    @Bean
    public Gauge activeLoansGauge(MeterRegistry registry) {
        return Gauge.builder("library.loans.active",
                        loanRepository, r -> r.countByStatus(LoanStatus.ACTIVE))
                .description("Currently active loans")
                .register(registry);
    }

    @Bean
    public Gauge pendingFinesGauge(MeterRegistry registry) {
        return Gauge.builder("library.fines.pending.count",
                        fineRepository, r -> r.countByStatus(FineStatus.PENDING))
                .description("Number of unpaid fines")
                .register(registry);
    }

    @Bean
    public Gauge waitingReservationsGauge(MeterRegistry registry) {
        return Gauge.builder("library.reservations.waiting",
                        reservationRepository, r -> r.countByStatus(ReservationStatus.WAITING))
                .description("Members waiting for a book")
                .register(registry);
    }
}