package com.library.management.domain.entity;

import com.library.management.domain.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
public class Loan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private LocalDate loanDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status = LoanStatus.ACTIVE;

    @Column(nullable = false)
    private int extensionCount = 0;

    @OneToOne(mappedBy = "loan", cascade = CascadeType.ALL)
    private Fine fine;

    public boolean isOverdue(LocalDate today) {
        LocalDate checkDate = returnDate != null ? returnDate : today;
        return checkDate.isAfter(dueDate);
    }

    public long overdueDays(LocalDate today) {
        if (!isOverdue(today)) return 0;
        LocalDate checkDate = returnDate != null ? returnDate : today;
        return ChronoUnit.DAYS.between(dueDate, checkDate);
    }

    public boolean isOverdue() {
        return isOverdue(LocalDate.now());
    }

    public long overdueDays() {
        return overdueDays(LocalDate.now());
    }
}
