package com.ferreteria.fc.domain.service.Impl;
import com.ferreteria.fc.domain.dto.response.DashboardResponse;
import com.ferreteria.fc.domain.repository.DashboardRepository;
import com.ferreteria.fc.domain.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DashboardRepository dashboardRepository;

    @Override
    public DashboardResponse getDashboard() {

        return DashboardResponse.builder()
                .ventasHoy(dashboardRepository.getVentasHoy())
                .pedidosPendientes(dashboardRepository.getPedidosPendientes())
                .clientes(dashboardRepository.getClientes())
                .productos(dashboardRepository.getProductos())
                .build();

    }

}
