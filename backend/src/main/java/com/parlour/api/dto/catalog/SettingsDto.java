package com.parlour.api.dto.catalog;

import com.parlour.api.entity.SiteSettings;
import jakarta.validation.constraints.*;

public record SettingsDto(
        @NotBlank @Size(max = 120) String parlourName,
        @NotBlank @Size(max = 200) String tagline,
        @Size(max = 2000) String about,
        @NotBlank @Pattern(regexp = "^\\+?[0-9 \\-]{7,20}$", message = "Enter a valid phone number") String phone,
        @Size(max = 200) String email,
        @Size(max = 500) String address,
        @Size(max = 500) String openingHours,
        @Size(max = 500) String offerText,
        @Size(max = 500) String heroImageUrl) {

    public static SettingsDto from(SiteSettings s) {
        return new SettingsDto(s.getParlourName(), s.getTagline(), s.getAbout(), s.getPhone(), s.getEmail(),
                s.getAddress(), s.getOpeningHours(), s.getOfferText(), s.getHeroImageUrl());
    }
}
