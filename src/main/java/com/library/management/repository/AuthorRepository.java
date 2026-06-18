package com.library.management.repository;

import com.library.management.domain.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author, Long> {

//    boolean existsByFirstNameAndLastName(String firstName, String lastName);
}
