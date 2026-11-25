package com.ferreteria.fc.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BrandRequest {
    @NotBlank
    @Size(max = 100)
    private String name;
}
