package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.domain.dto.response.DashboardOrderStatusResponse;
import com.ferreteria.fc.domain.dto.response.DashboardProductStatusResponse;
import com.ferreteria.fc.domain.dto.response.DashboardResponse;

import java.time.LocalDateTime;

public interface DashboardService {

    DashboardResponse getDashboard();

    DashboardOrderStatusResponse getOrdersByStatusAndPeriod(
            Long storeId,
            LocalDateTime from,
            LocalDateTime to
    );

    DashboardProductStatusResponse getProductStatusByStore(Long storeId);
}
