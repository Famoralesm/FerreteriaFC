package com.ferreteria.fc.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SetBaseUnitRequest {
    @NotNull
    private Long unitOfMeasureId;

    private boolean preserveExistingValues;
}
