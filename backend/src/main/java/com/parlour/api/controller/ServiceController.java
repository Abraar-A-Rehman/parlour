package com.parlour.api.controller;

import com.parlour.api.dto.catalog.ServiceDto;
import com.parlour.api.dto.catalog.ServiceRequest;
import com.parlour.api.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Parlour services. Reading is public; creating, editing and deleting is OWNER only (see SecurityConfig). */
@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final CatalogService catalog;

    public ServiceController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<ServiceDto> list() {
        return catalog.publicServices();
    }

    @PostMapping
    public ServiceDto create(@Valid @RequestBody ServiceRequest req) {
        return catalog.createService(req);
    }

    @PutMapping("/{id}")
    public ServiceDto update(@PathVariable Long id, @Valid @RequestBody ServiceRequest req) {
        return catalog.updateService(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        catalog.deleteService(id);
        return ResponseEntity.noContent().build();
    }
}
