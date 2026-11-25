package com.ferreteria.fc.domain.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class BrandResponse {
    private Long id;
    private String name;
}
