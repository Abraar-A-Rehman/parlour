package com.parlour.api.dto.auth;

import jakarta.validation.constraints.*;

public record RefreshRequest(@NotBlank String refreshToken) {
}
