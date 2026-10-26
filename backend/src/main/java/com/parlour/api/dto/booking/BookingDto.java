package com.parlour.api.dto.booking;

import com.parlour.api.entity.BookedVia;
import com.parlour.api.entity.Booking;
import com.parlour.api.entity.BookingStatus;
import java.time.LocalDateTime;

/** Admin view of a booking. Contains customer data, so it is only served to OWNER / MANAGER. */
public record BookingDto(Long id, String customerName, String customerPhone, Long serviceId, String serviceName,
                         LocalDateTime startTime, LocalDateTime endTime, BookingStatus status,
                         BookedVia bookedVia, String notes) {

    public static BookingDto from(Booking b) {
        return new BookingDto(b.getId(), b.getCustomerName(), b.getCustomerPhone(), b.getService().getId(),
                b.getService().getName(), b.getStartTime(), b.getEndTime(), b.getStatus(), b.getBookedVia(),
                b.getNotes());
    }
}
