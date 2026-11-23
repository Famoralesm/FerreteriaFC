package com.ferreteria.fc.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardOrderStatusResponse {

    private Long total;
    private Long created;
    private Long preparing;
    private Long inRoute;
    private Long delivered;
    private Long rejected;
    private Long canceled;
}
