package com.parlour.api.dto.catalog;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ServiceRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 60) String category,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin("0.0") @DecimalMax("1000000") BigDecimal price,
        @Min(5) @Max(480) int durationMin,
        @Size(max = 500) String imageUrl,
        Boolean active) {
}
