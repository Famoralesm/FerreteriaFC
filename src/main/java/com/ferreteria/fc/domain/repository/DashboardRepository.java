package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.persistence.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface DashboardRepository extends JpaRepository<Order, Long> {

    @Query(value = """
            SELECT COALESCE(SUM(total_amount), 0)
            FROM orders
            WHERE created_at >= CURRENT_DATE
              AND created_at < CURRENT_DATE + INTERVAL '1 day'
            """, nativeQuery = true)
    BigDecimal getVentasHoy();

    @Query(value = """
            SELECT COUNT(*)
            FROM orders
            WHERE status IN ('CREATED', 'PREPARING', 'IN_ROUTE')
            """, nativeQuery = true)
    Long getPedidosPendientes();

    @Query(value = """
            SELECT COUNT(*)
            FROM customers
            """, nativeQuery = true)
    Long getClientes();

    @Query(value = """
            SELECT COUNT(*)
            FROM products
            """, nativeQuery = true)
    Long getProductos();
}
