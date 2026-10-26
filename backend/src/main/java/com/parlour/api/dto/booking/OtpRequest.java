package com.parlour.api.dto.booking;

import jakarta.validation.constraints.*;

public record OtpRequest(
        @NotBlank @Pattern(regexp = "^\\+?[0-9 \\-]{10,18}$", message = "Enter a valid phone number") String phone) {
}
