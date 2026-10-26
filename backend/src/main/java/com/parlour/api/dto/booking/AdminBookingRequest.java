package com.parlour.api.dto.booking;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record AdminBookingRequest(
        @NotBlank @Size(max = 100) String customerName,
        @NotBlank @Pattern(regexp = "^\\+?[0-9 \\-]{10,18}$", message = "Enter a valid phone number") String customerPhone,
        @NotNull Long serviceId,
        @NotNull LocalDateTime startTime,
        @Size(max = 500) String notes) {
}
