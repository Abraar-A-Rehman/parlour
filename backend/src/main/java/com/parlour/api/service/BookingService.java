package com.parlour.api.service;

import com.parlour.api.entity.*;
import com.parlour.api.repository.BookingRepository;
import com.parlour.api.repository.ServiceRepository;
import com.parlour.api.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookingService {

    private static final DateTimeFormatter HUMAN = DateTimeFormatter.ofPattern("EEE d MMM, h:mm a");
    private static final int MAX_DAYS_AHEAD = 90;

    private final BookingRepository bookings;
    private final ServiceRepository services;
    private final OtpService otpService;
    private final SmsSender sms;
    private final LocalTime open;
    private final LocalTime close;
    private final int slotMinutes;
    private final int capacity;

    public BookingService(BookingRepository bookings, ServiceRepository services, OtpService otpService,
                          SmsSender sms,
                          @Value("${app.booking.open-time}") String open,
                          @Value("${app.booking.close-time}") String close,
                          @Value("${app.booking.slot-minutes}") int slotMinutes,
                          @Value("${app.booking.capacity}") int capacity) {
        this.bookings = bookings;
        this.services = services;
        this.otpService = otpService;
        this.sms = sms;
        this.open = LocalTime.parse(open);
        this.close = LocalTime.parse(close);
        this.slotMinutes = slotMinutes;
        this.capacity = capacity;
    }

    // ---------------------------------------------------------------- customer side

    public static String normalizePhone(String phone) {
        return phone.replaceAll("[\\s-]", "");
    }

    /** Step 1 of customer booking: text an OTP to the number the customer typed. */
    public java.util.Optional<String> requestCustomerOtp(String phone) {
        String p = normalizePhone(phone);
        return otpService.issue("customer:" + p, p);
    }

    /** Step 2: the OTP proves the customer owns the number, then the booking is stored. */
    @Transactional(noRollbackFor = ApiException.class) // keep the wrong-attempt counter
    public synchronized Booking createOnline(String name, String phone, Long serviceId, LocalDateTime start, String otp) {
        String p = normalizePhone(phone);
        Slot slot = checkSlot(serviceId, start);       // reject bad/taken slots before spending the OTP
        otpService.verify("customer:" + p, otp);
        Booking b = save(name.trim(), p, slot, BookedVia.CALENDAR, null, BookingStatus.PENDING);
        sms.send(p, "Booking received for " + b.getService().getName() + " on " + b.getStartTime().format(HUMAN)
                + ". We will confirm shortly.");
        return b;
    }

    /** Free start times ("HH:mm") for a service on a date. */
    @Transactional(readOnly = true)
    public List<String> availableSlots(LocalDate date, Long serviceId) {
        ServiceItem service = activeService(serviceId);
        if (date.isBefore(LocalDate.now()) || date.isAfter(LocalDate.now().plusDays(MAX_DAYS_AHEAD))) {
            return List.of();
        }
        List<Booking> sameDay = bookings.findActiveOverlapping(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        LocalDateTime now = LocalDateTime.now();
        List<String> result = new ArrayList<>();
        for (LocalTime t = open; t.isBefore(close); t = t.plusMinutes(slotMinutes)) {
            if (!fitsOpeningHours(t, service.getDurationMin())) {
                continue;
            }
            LocalDateTime start = date.atTime(t);
            LocalDateTime end = start.plusMinutes(service.getDurationMin());
            if (!start.isAfter(now)) {
                continue;
            }
            long overlapping = sameDay.stream().filter(b -> b.getStartTime().isBefore(end) && b.getEndTime().isAfter(start)).count();
            if (overlapping < capacity) {
                result.add(t.toString());
            }
        }
        return result;
    }

    // ---------------------------------------------------------------- admin side

    @Transactional
    public synchronized Booking createByStaff(String name, String phone, Long serviceId, LocalDateTime start, String notes) {
        Slot slot = checkSlot(serviceId, start);
        return save(name.trim(), normalizePhone(phone), slot, BookedVia.CALL, notes, BookingStatus.CONFIRMED);
    }

    @Transactional(readOnly = true)
    public List<Booking> list(LocalDate date) {
        if (date == null) {
            return bookings.findAllByOrderByStartTimeDesc();
        }
        return bookings.findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
                date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    @Transactional
    public Booking updateStatus(Long id, BookingStatus status) {
        Booking b = bookings.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (b.getStatus() == BookingStatus.CANCELLED || b.getStatus() == BookingStatus.COMPLETED) {
            // a cancelled booking must not come back (its slot may already be taken by someone else)
            throw new ApiException(HttpStatus.BAD_REQUEST, "A " + b.getStatus().name().toLowerCase() + " booking cannot be changed");
        }
        b.setStatus(status);
        if (status == BookingStatus.CONFIRMED) {
            sms.send(b.getCustomerPhone(), "Your appointment for " + b.getService().getName() + " on "
                    + b.getStartTime().format(HUMAN) + " is confirmed.");
        } else if (status == BookingStatus.CANCELLED) {
            sms.send(b.getCustomerPhone(), "Your appointment on " + b.getStartTime().format(HUMAN)
                    + " has been cancelled. Please call us to rebook.");
        }
        return b;
    }

    // ---------------------------------------------------------------- shared rules

    private record Slot(ServiceItem service, LocalDateTime start, LocalDateTime end) {
    }

    /**
     * Validates the requested slot. The public methods are synchronized, so with the capacity check
     * two people cannot grab the last slot at the same time (fine for a single local server).
     */
    private Slot checkSlot(Long serviceId, LocalDateTime start) {
        ServiceItem service = activeService(serviceId);
        LocalDateTime now = LocalDateTime.now();

        if (!start.isAfter(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please choose a time in the future");
        }
        if (start.isAfter(now.plusDays(MAX_DAYS_AHEAD))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bookings open up to " + MAX_DAYS_AHEAD + " days ahead");
        }
        LocalTime t = start.toLocalTime();
        long minutesFromOpen = Duration.between(open, t).toMinutes();
        if (t.getSecond() != 0 || t.getNano() != 0 || minutesFromOpen < 0 || minutesFromOpen % slotMinutes != 0
                || !fitsOpeningHours(t, service.getDurationMin())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "That time is outside our opening hours");
        }

        LocalDateTime end = start.plusMinutes(service.getDurationMin());
        if (bookings.findActiveOverlapping(start, end).size() >= capacity) {
            throw new ApiException(HttpStatus.CONFLICT, "That time was just taken. Please choose another slot");
        }

        return new Slot(service, start, end);
    }

    private Booking save(String name, String phone, Slot slot, BookedVia via, String notes, BookingStatus status) {
        Booking b = new Booking();
        b.setCustomerName(name);
        b.setCustomerPhone(phone);
        b.setService(slot.service());
        b.setStartTime(slot.start());
        b.setEndTime(slot.end());
        b.setBookedVia(via);
        b.setNotes(notes);
        b.setStatus(status);
        return bookings.save(b);
    }

    private boolean fitsOpeningHours(LocalTime start, int durationMin) {
        long minutesUntilClose = Duration.between(start, close).toMinutes();
        return minutesUntilClose >= durationMin;
    }

    private ServiceItem activeService(Long id) {
        return services.findById(id).filter(ServiceItem::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Service not found"));
    }
}
