package com.library.management.domain.entity;

import com.library.management.domain.enums.FineStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fines")
@Getter
@Setter
@NoArgsConstructor
public class Fine extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false, unique = true)
    private Loan loan;

    @Column(nullable = false)
    private long amount;   // in smallest currency unit (tiyin)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FineStatus status = FineStatus.PENDING;

    private LocalDateTime paidAt;

    @Column(nullable = false)
    private LocalDate calculatedUpTo;  // idempotency: last date fine was calculated for
}
