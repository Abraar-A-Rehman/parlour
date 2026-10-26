package com.parlour.api.dto.auth;

import com.parlour.api.entity.Role;

public record TokenResponse(String accessToken, String refreshToken, Role role, String email, String fullName) {
}
