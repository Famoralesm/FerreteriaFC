package com.ferreteria.fc.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UnitOfMeasureRequest {
    @NotBlank @Size(max = 30)
    private String code;
    @NotBlank @Size(max = 80)
    private String name;
    @NotBlank @Size(max = 12)
    private String symbol;
}