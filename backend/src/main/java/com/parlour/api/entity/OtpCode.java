package com.parlour.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** One-time password. Only a BCrypt hash of the code is stored. */
@Entity
@Table(name = "otp_codes", indexes = @Index(name = "idx_otp_subject", columnList = "subject"))
@Getter
@Setter
public class OtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g. "admin:owner@parlour.local" or "customer:+8801712345678" */
    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String codeHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private int attempts = 0;
}
