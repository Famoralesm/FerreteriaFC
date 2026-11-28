package com.ferreteria.fc.domain.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommercialInvoiceRequest {
    @NotNull
    private Long storeId;

    @NotBlank
    @Size(max = 200)
    private String customerName;

    @Size(max = 12)
    private String customerRut;

    @Size(max = 200)
    private String customerBusinessName;

    @Size(max = 150)
    private String customerGiro;

    @Size(max = 250)
    private String customerAddress;

    @Size(max = 150)
    private String customerCity;

    @Size(max = 150)
    private String customerCommune;

    @Size(max = 150)
    private String customerEmail;

    @Size(max = 30)
    private String customerPhone;

    private String notes;

    @NotEmpty
    @Valid
    private List<CommercialInvoiceItemRequest> items;
}
