package com.parlour.api.dto.booking;

import com.parlour.api.entity.BookingStatus;
import jakarta.validation.constraints.*;

public record StatusRequest(@NotNull BookingStatus status) {
}
