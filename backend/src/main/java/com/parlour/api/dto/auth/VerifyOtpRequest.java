package com.parlour.api.dto.auth;

import jakarta.validation.constraints.*;

public record VerifyOtpRequest(@NotBlank @Email String email, @NotBlank @Pattern(regexp = "\\d{4,8}") String code) {
}
