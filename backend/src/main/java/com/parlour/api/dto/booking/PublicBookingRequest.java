package com.parlour.api.dto.booking;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record PublicBookingRequest(
        @NotBlank @Size(max = 100) String customerName,
        @NotBlank @Pattern(regexp = "^\\+?[0-9 \\-]{10,18}$", message = "Enter a valid phone number") String customerPhone,
        @NotNull Long serviceId,
        @NotNull LocalDateTime startTime,
        @NotBlank @Pattern(regexp = "\\d{4,8}") String otp) {
}
