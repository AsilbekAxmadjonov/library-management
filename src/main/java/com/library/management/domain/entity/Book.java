package com.library.management.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
public class Book extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(unique = true, length = 20)
    private String isbn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    @Column(nullable = false)
    private int totalCopies;       // total physical copies

    @Column(nullable = false)
    private int availableCopies;// copies not currently loaned out

    @Column(nullable = false)
    private int reservedCopies;

    @Column(length = 100)
    private String genre;

    @Column(nullable = false)
    private int publicationYear;

    // For fine cap (Task 6 bonus): book price
    private Long price;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    private List<Loan> loans = new ArrayList<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    private List<Reservation> reservations = new ArrayList<>();
}
