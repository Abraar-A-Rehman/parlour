package com.parlour.api.dto.booking;

import com.parlour.api.entity.Booking;
import com.parlour.api.entity.BookingStatus;
import java.time.LocalDateTime;

/** What a public customer gets back: only their own booking, no personal data. */
public record BookingConfirmation(Long id, String serviceName, LocalDateTime startTime, LocalDateTime endTime,
                                  BookingStatus status) {

    public static BookingConfirmation from(Booking b) {
        return new BookingConfirmation(b.getId(), b.getService().getName(), b.getStartTime(), b.getEndTime(),
                b.getStatus());
    }
}
