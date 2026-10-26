package com.parlour.api.repository;

import com.parlour.api.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpCode, Long> {
    Optional<OtpCode> findFirstBySubjectOrderByIdDesc(String subject);

    void deleteBySubject(String subject);
}
