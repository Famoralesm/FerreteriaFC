package com.ferreteria.fc.domain.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;

    private String sku;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private Boolean active;

    private String category;

    private String brand;

    private String unit;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}