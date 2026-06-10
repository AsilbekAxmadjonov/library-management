package com.library.management.repository;

import com.library.management.domain.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// BookRepository.java
public interface BookRepository extends JpaRepository<Book, Long> {

    // Task 7: search with pagination
    @Query("""
        SELECT b FROM Book b JOIN b.author a
        WHERE (:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%')))
        AND   (:authorName IS NULL OR LOWER(a.firstName) LIKE LOWER(CONCAT('%', :authorName, '%'))
                                   OR LOWER(a.lastName)  LIKE LOWER(CONCAT('%', :authorName, '%')))
        AND   (:genre IS NULL OR b.genre = :genre)
    """)
    Page<Book> searchBooks(
            @Param("title") String title,
            @Param("authorName") String authorName,
            @Param("genre") String genre,
            Pageable pageable
    );

    // Task 6 report: most read books
    @Query("""
        SELECT b, COUNT(l) as loanCount 
        FROM Book b LEFT JOIN b.loans l 
        GROUP BY b 
        ORDER BY loanCount DESC
    """)
    List<Object[]> findMostReadBooks(Pageable pageable);
}
