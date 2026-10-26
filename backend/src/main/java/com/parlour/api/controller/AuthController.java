package com.parlour.api.controller;

import com.parlour.api.dto.auth.LoginRequest;
import com.parlour.api.dto.auth.LoginResponse;
import com.parlour.api.dto.auth.RefreshRequest;
import com.parlour.api.dto.auth.TokenResponse;
import com.parlour.api.dto.auth.VerifyOtpRequest;
import com.parlour.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Staff (owner / manager) authentication: password + OTP, token refresh, logout. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    /** Step 1: email + password. On success an OTP is sent to the admin's phone. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req.email(), req.password());
    }

    /** Step 2: OTP. Returns the access token and the refresh token. */
    @PostMapping("/verify-otp")
    public TokenResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        return auth.verifyOtp(req.email(), req.code());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return auth.refresh(req.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req) {
        auth.logout(req.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
