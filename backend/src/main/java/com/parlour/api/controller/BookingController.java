package com.parlour.api.controller;

import com.parlour.api.dto.booking.BookingConfirmation;
import com.parlour.api.dto.booking.OtpRequest;
import com.parlour.api.dto.booking.OtpSentResponse;
import com.parlour.api.dto.booking.PublicBookingRequest;
import com.parlour.api.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Public endpoints used by customers to book online. Staff endpoints are in controller/admin. */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookings;

    public BookingController(BookingService bookings) {
        this.bookings = bookings;
    }

    /** Free start times ("HH:mm") for a service on a date. */
    @GetMapping("/availability")
    public List<String> availability(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                     @RequestParam Long serviceId) {
        return bookings.availableSlots(date, serviceId);
    }

    /** Step 1: text an OTP to the customer's phone. */
    @PostMapping("/request-otp")
    public OtpSentResponse requestOtp(@Valid @RequestBody OtpRequest req) {
        return new OtpSentResponse(true, bookings.requestCustomerOtp(req.phone()).orElse(null));
    }

    /** Step 2: create the booking, proving phone ownership with the OTP. */
    @PostMapping
    public BookingConfirmation book(@Valid @RequestBody PublicBookingRequest req) {
        return BookingConfirmation.from(bookings.createOnline(
                req.customerName(), req.customerPhone(), req.serviceId(), req.startTime(), req.otp()));
    }
}
