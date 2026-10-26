package com.parlour.api.service;

import com.parlour.api.entity.OtpCode;
import com.parlour.api.repository.OtpRepository;
import com.parlour.api.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * One-time passwords sent by SMS. Rules: 6 digits, expires after a few minutes, max 3 wrong attempts,
 * a new code can only be requested after a short cool-down. Only a BCrypt hash is stored.
 */
@Service
public class OtpService {

    private final OtpRepository repo;
    private final PasswordEncoder encoder;
    private final SmsSender sms;
    private final int length;
    private final int expiryMinutes;
    private final int maxAttempts;
    private final int cooldownSeconds;
    private final boolean devEcho;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpRepository repo, PasswordEncoder encoder, SmsSender sms,
                      @Value("${app.otp.length}") int length,
                      @Value("${app.otp.expiry-minutes}") int expiryMinutes,
                      @Value("${app.otp.max-attempts}") int maxAttempts,
                      @Value("${app.otp.resend-cooldown-seconds}") int cooldownSeconds,
                      @Value("${app.otp.dev-echo}") boolean devEcho) {
        this.repo = repo;
        this.encoder = encoder;
        this.sms = sms;
        this.length = length;
        this.expiryMinutes = expiryMinutes;
        this.maxAttempts = maxAttempts;
        this.cooldownSeconds = cooldownSeconds;
        this.devEcho = devEcho;
    }

    /** Creates a code for the subject and texts it to the phone. Returns the code only in dev-echo mode. */
    @Transactional
    public Optional<String> issue(String subject, String phone) {
        LocalDateTime now = LocalDateTime.now();
        repo.findFirstBySubjectOrderByIdDesc(subject).ifPresent(previous -> {
            if (cooldownSeconds > 0 && previous.getCreatedAt().plusSeconds(cooldownSeconds).isAfter(now)) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait " + cooldownSeconds + " seconds before requesting another code");
            }
        });
        repo.deleteBySubject(subject);

        String code = generateCode();
        OtpCode otp = new OtpCode();
        otp.setSubject(subject);
        otp.setCodeHash(encoder.encode(code));
        otp.setExpiresAt(now.plusMinutes(expiryMinutes));
        repo.save(otp);

        sms.send(phone, "Your verification code is " + code + ". It expires in " + expiryMinutes + " minutes.");
        return devEcho ? Optional.of(code) : Optional.empty();
    }

    /** Throws ApiException unless the code is correct. A used code can never be used again. */
    @Transactional(noRollbackFor = ApiException.class) // wrong-attempt counter must be saved even though we throw
    public void verify(String subject, String code) {
        OtpCode otp = repo.findFirstBySubjectOrderByIdDesc(subject)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Code expired or not requested"));

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            repo.delete(otp);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Code expired. Request a new one");
        }
        if (otp.getAttempts() >= maxAttempts) {
            repo.delete(otp);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many wrong attempts. Request a new code");
        }
        if (!encoder.matches(code, otp.getCodeHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            repo.save(otp);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Incorrect code");
        }
        repo.delete(otp);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
