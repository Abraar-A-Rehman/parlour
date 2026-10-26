package com.parlour.api.dto.auth;

public record LoginResponse(boolean otpRequired, String maskedPhone, String devOtp) {
}
