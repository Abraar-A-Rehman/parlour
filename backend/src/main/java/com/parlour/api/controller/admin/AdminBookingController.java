package com.parlour.api.controller.admin;

import com.parlour.api.dto.booking.AdminBookingRequest;
import com.parlour.api.dto.booking.BookingDto;
import com.parlour.api.dto.booking.StatusRequest;
import com.parlour.api.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Bookings dashboard for OWNER and MANAGER (everything under /api/admin requires one of those roles). */
@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {

    private final BookingService bookings;

    public AdminBookingController(BookingService bookings) {
        this.bookings = bookings;
    }

    @GetMapping
    public List<BookingDto> list(@RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookings.list(date).stream().map(BookingDto::from).toList();
    }

    /** A customer phoned: staff enters the booking by hand. */
    @PostMapping
    public BookingDto createByPhone(@Valid @RequestBody AdminBookingRequest req) {
        return BookingDto.from(bookings.createByStaff(
                req.customerName(), req.customerPhone(), req.serviceId(), req.startTime(), req.notes()));
    }

    @PatchMapping("/{id}/status")
    public BookingDto setStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        return BookingDto.from(bookings.updateStatus(id, req.status()));
    }
}
