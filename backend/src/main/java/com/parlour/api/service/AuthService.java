package com.parlour.api.service;

import com.parlour.api.dto.auth.LoginResponse;
import com.parlour.api.dto.auth.TokenResponse;
import com.parlour.api.entity.RefreshToken;
import com.parlour.api.entity.User;
import com.parlour.api.repository.RefreshTokenRepository;
import com.parlour.api.repository.UserRepository;
import com.parlour.api.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Admin login in two steps:
 * 1) email + password  -> an OTP is texted to the admin's phone
 * 2) OTP               -> access token (15 min) + refresh token (7 days)
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final LoginThrottle throttle;
    private final long refreshDays;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder encoder,
                       OtpService otpService, JwtService jwtService, LoginThrottle throttle,
                       @Value("${app.jwt.refresh-days}") long refreshDays) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.otpService = otpService;
        this.jwtService = jwtService;
        this.throttle = throttle;
        this.refreshDays = refreshDays;
    }

    @Transactional
    public LoginResponse login(String email, String password) {
        throttle.assertNotLocked(email);
        User user = users.findByEmailIgnoreCase(email)
                .filter(User::isEnabled)
                .filter(u -> encoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> {
                    throttle.recordFailure(email);
                    return new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
                });
        throttle.reset(email);
        String devOtp = otpService.issue(subject(user.getEmail()), user.getPhone()).orElse(null);
        return new LoginResponse(true, mask(user.getPhone()), devOtp);
    }

    @Transactional(noRollbackFor = ApiException.class) // keep the wrong-attempt counter
    public TokenResponse verifyOtp(String email, String code) {
        User user = users.findByEmailIgnoreCase(email)
                .filter(User::isEnabled)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Code expired or not requested"));
        otpService.verify(subject(user.getEmail()), code);
        return issueTokens(user);
    }

    /** Refresh-token rotation: every refresh returns a NEW refresh token and revokes the old one. */
    @Transactional(noRollbackFor = ApiException.class)
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokens.findByTokenHash(sha256(rawRefreshToken))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (stored.isRevoked()) {
            // An old token was used again: it may have been stolen, so end every session of this user.
            refreshTokens.findByUser(stored.getUser()).forEach(t -> t.setRevoked(true));
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Session expired, please log in again");
        }
        if (stored.getExpiresAt().isBefore(Instant.now()) || !stored.getUser().isEnabled()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Session expired, please log in again");
        }
        stored.setRevoked(true);
        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.findByTokenHash(sha256(rawRefreshToken)).ifPresent(t -> t.setRevoked(true));
    }

    private TokenResponse issueTokens(User user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String rawRefresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(sha256(rawRefresh));
        rt.setExpiresAt(Instant.now().plus(refreshDays, ChronoUnit.DAYS));
        refreshTokens.save(rt);

        return new TokenResponse(jwtService.createAccessToken(user), rawRefresh, user.getRole(), user.getEmail(),
                user.getFullName());
    }

    private static String subject(String email) {
        return "admin:" + email.toLowerCase();
    }

    private static String mask(String phone) {
        if (phone.length() <= 4) {
            return "****";
        }
        return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
