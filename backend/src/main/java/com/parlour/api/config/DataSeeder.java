package com.parlour.api.config;

import com.parlour.api.entity.*;
import com.parlour.api.repository.ServiceRepository;
import com.parlour.api.repository.SiteSettingsRepository;
import com.parlour.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Creates demo data on the very first start (empty database). */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final ServiceRepository services;
    private final SiteSettingsRepository settings;
    private final PasswordEncoder encoder;

    @Value("${app.seed.owner-email}") private String ownerEmail;
    @Value("${app.seed.owner-password}") private String ownerPassword;
    @Value("${app.seed.owner-phone}") private String ownerPhone;
    @Value("${app.seed.manager-email}") private String managerEmail;
    @Value("${app.seed.manager-password}") private String managerPassword;
    @Value("${app.seed.manager-phone}") private String managerPhone;

    public DataSeeder(UserRepository users, ServiceRepository services, SiteSettingsRepository settings,
                      PasswordEncoder encoder) {
        this.users = users;
        this.services = services;
        this.settings = settings;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() == 0) {
            users.save(user(ownerEmail, ownerPassword, ownerPhone, "Parlour Owner", Role.OWNER));
            users.save(user(managerEmail, managerPassword, managerPhone, "Parlour Manager", Role.MANAGER));
            log.info("Seeded owner ({}) and manager ({}) accounts", ownerEmail, managerEmail);
        }
        if (services.count() == 0) {
            services.save(service("Haircut & Styling", "Hair", "Consultation, wash, precision cut and blow-dry.", "800", 45));
            services.save(service("Hair Colour", "Hair", "Full colour or highlights with premium, low-ammonia products.", "3500", 120));
            services.save(service("Keratin Treatment", "Hair", "Smoothing treatment for frizz-free, shiny hair that lasts for months.", "6000", 150));
            services.save(service("Hair Spa", "Hair", "Deep conditioning spa with scalp massage.", "1800", 60));
            services.save(service("Glow Facial", "Skin", "Deep cleanse, exfoliation, massage and a brightening mask.", "2000", 60));
            services.save(service("Threading & Waxing", "Skin", "Gentle hair removal for face and body.", "600", 30));
            services.save(service("Classic Manicure", "Nails", "Shaping, cuticle care, massage and polish.", "700", 30));
            services.save(service("Spa Pedicure", "Nails", "Soak, scrub, massage and polish for soft, healthy feet.", "900", 60));
            services.save(service("Bridal Makeup", "Makeup", "Complete bridal look with trial consultation included.", "12000", 180));
        }
        if (settings.count() == 0) {
            SiteSettings s = new SiteSettings();
            s.setParlourName("Bella Luxe Beauty Parlour");
            s.setTagline("Feel beautiful. Feel like yourself.");
            s.setAbout("We are a friendly neighbourhood parlour offering hair, skin, nails and bridal services by "
                    + "experienced stylists, using trusted products in a clean and relaxing space.");
            s.setPhone("+880 1700-000000");
            s.setEmail("hello@bellaluxe.example");
            s.setAddress("House 12, Road 5, Dhanmondi, Dhaka");
            s.setOpeningHours("Every day, 10:00 AM - 8:00 PM");
            s.setOfferText("New here? Get 15% off your first visit. Mention it when you arrive.");
            settings.save(s);
        }
    }

    private User user(String email, String password, String phone, String name, Role role) {
        User u = new User();
        u.setEmail(email.toLowerCase());
        u.setPasswordHash(encoder.encode(password));
        u.setPhone(phone);
        u.setFullName(name);
        u.setRole(role);
        return u;
    }

    private ServiceItem service(String name, String category, String description, String price, int minutes) {
        ServiceItem s = new ServiceItem();
        s.setName(name);
        s.setCategory(category);
        s.setDescription(description);
        s.setPrice(new BigDecimal(price));
        s.setDurationMin(minutes);
        return s;
    }
}
