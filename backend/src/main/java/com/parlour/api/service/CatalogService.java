package com.parlour.api.service;

import com.parlour.api.dto.catalog.ServiceDto;
import com.parlour.api.dto.catalog.ServiceRequest;
import com.parlour.api.dto.catalog.SettingsDto;
import com.parlour.api.entity.ServiceItem;
import com.parlour.api.entity.SiteSettings;
import com.parlour.api.repository.BookingRepository;
import com.parlour.api.repository.ServiceRepository;
import com.parlour.api.repository.SiteSettingsRepository;
import com.parlour.api.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Everything the owner can edit on the website: services and the site texts. */
@Service
public class CatalogService {

    private final ServiceRepository services;
    private final BookingRepository bookings;
    private final SiteSettingsRepository settings;

    public CatalogService(ServiceRepository services, BookingRepository bookings, SiteSettingsRepository settings) {
        this.services = services;
        this.bookings = bookings;
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public List<ServiceDto> publicServices() {
        return services.findByActiveTrueOrderByCategoryAscNameAsc().stream().map(ServiceDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceDto> allServices() {
        return services.findAllByOrderByCategoryAscNameAsc().stream().map(ServiceDto::from).toList();
    }

    @Transactional
    public ServiceDto createService(ServiceRequest r) {
        ServiceItem s = new ServiceItem();
        apply(s, r);
        return ServiceDto.from(services.save(s));
    }

    @Transactional
    public ServiceDto updateService(Long id, ServiceRequest r) {
        ServiceItem s = services.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Service not found"));
        apply(s, r);
        return ServiceDto.from(s);
    }

    /** Deletes a service. If customers already booked it, it is only hidden so old bookings stay valid. */
    @Transactional
    public void deleteService(Long id) {
        ServiceItem s = services.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Service not found"));
        if (bookings.existsByServiceId(id)) {
            s.setActive(false);
        } else {
            services.delete(s);
        }
    }

    @Transactional(readOnly = true)
    public SettingsDto getSettings() {
        return SettingsDto.from(settings.findById(1L).orElseThrow());
    }

    @Transactional
    public SettingsDto updateSettings(SettingsDto d) {
        SiteSettings s = settings.findById(1L).orElseThrow();
        s.setParlourName(d.parlourName().trim());
        s.setTagline(d.tagline().trim());
        s.setAbout(d.about());
        s.setPhone(d.phone().trim());
        s.setEmail(d.email());
        s.setAddress(d.address());
        s.setOpeningHours(d.openingHours());
        s.setOfferText(d.offerText());
        s.setHeroImageUrl(blankToNull(d.heroImageUrl()));
        return SettingsDto.from(s);
    }

    private void apply(ServiceItem s, ServiceRequest r) {
        s.setName(r.name().trim());
        s.setCategory(r.category().trim());
        s.setDescription(r.description());
        s.setPrice(r.price());
        s.setDurationMin(r.durationMin());
        s.setImageUrl(blankToNull(r.imageUrl()));
        s.setActive(r.active() == null || r.active());
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
