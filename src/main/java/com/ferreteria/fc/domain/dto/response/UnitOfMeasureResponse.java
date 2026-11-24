package com.ferreteria.fc.domain.dto.response;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UnitOfMeasureResponse {
    private Long id;
    private String code;
    private String name;
    private String symbol;
    private Boolean active;
}