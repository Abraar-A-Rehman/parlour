package com.parlour.api.repository;

import com.parlour.api.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findAllByOrderByStartTimeDesc();

    List<Booking> findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
            LocalDateTime from, LocalDateTime to);

    boolean existsByServiceId(Long serviceId);

    /** Bookings (not cancelled) that overlap the half-open interval [start, end). */
    @Query("""
            select b from Booking b
            where b.status <> com.parlour.api.entity.BookingStatus.CANCELLED
              and b.startTime < :end and b.endTime > :start
            """)
    List<Booking> findActiveOverlapping(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
