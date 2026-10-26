package com.parlour.api.controller;

import com.parlour.api.dto.catalog.SettingsDto;
import com.parlour.api.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Editable website texts (name, tagline, hours ...). Reading is public; editing is OWNER only. */
@RestController
@RequestMapping("/api/settings")
public class SiteSettingsController {

    private final CatalogService catalog;

    public SiteSettingsController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public SettingsDto get() {
        return catalog.getSettings();
    }

    @PutMapping
    public SettingsDto update(@Valid @RequestBody SettingsDto req) {
        return catalog.updateSettings(req);
    }
}
