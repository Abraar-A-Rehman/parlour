package com.parlour.api.controller.admin;

import com.parlour.api.dto.catalog.ServiceDto;
import com.parlour.api.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Staff view of services, including hidden ones. */
@RestController
@RequestMapping("/api/admin/services")
public class AdminServiceController {

    private final CatalogService catalog;

    public AdminServiceController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<ServiceDto> listAll() {
        return catalog.allServices();
    }
}
