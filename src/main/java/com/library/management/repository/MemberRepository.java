package com.library.management.repository;

import com.library.management.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

// repository/MemberRepository.java
public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmail(String email);

}
