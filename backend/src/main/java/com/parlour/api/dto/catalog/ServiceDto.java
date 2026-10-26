package com.parlour.api.dto.catalog;

import com.parlour.api.entity.ServiceItem;
import java.math.BigDecimal;

public record ServiceDto(Long id, String name, String category, String description, BigDecimal price,
                         int durationMin, String imageUrl, boolean active) {

    public static ServiceDto from(ServiceItem s) {
        return new ServiceDto(s.getId(), s.getName(), s.getCategory(), s.getDescription(), s.getPrice(),
                s.getDurationMin(), s.getImageUrl(), s.isActive());
    }
}
