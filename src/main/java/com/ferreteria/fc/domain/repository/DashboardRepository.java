package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.domain.dto.response.DashboardOrderStatusResponse;
import com.ferreteria.fc.domain.dto.response.DashboardProductStatusResponse;
import com.ferreteria.fc.persistence.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface DashboardRepository extends JpaRepository<Order, Long> {

    @Query(value = """
            SELECT COALESCE(SUM(total_amount), 0)
            FROM orders
            WHERE payment_status = 'PAID'
              AND created_at >= CURRENT_DATE
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

    @Query("""
            SELECT new com.ferreteria.fc.domain.dto.response.DashboardOrderStatusResponse(
                COUNT(o),
                COALESCE(SUM(CASE WHEN o.status = com.ferreteria.fc.domain.enums.OrderStatus.CREATED THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN o.status = com.ferreteria.fc.domain.enums.OrderStatus.PREPARING THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN o.status = com.ferreteria.fc.domain.enums.OrderStatus.IN_ROUTE THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN o.status = com.ferreteria.fc.domain.enums.OrderStatus.DELIVERED THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN o.status = com.ferreteria.fc.domain.enums.OrderStatus.REJECTED THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN o.status = com.ferreteria.fc.domain.enums.OrderStatus.CANCELED THEN 1L ELSE 0L END), 0L)
            )
            FROM Order o
            WHERE o.store.id = :storeId
              AND o.createdAt >= :from
              AND o.createdAt < :to
            """)
    DashboardOrderStatusResponse getOrdersByStatusAndPeriod(
            @Param("storeId") Long storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
            SELECT new com.ferreteria.fc.domain.dto.response.DashboardProductStatusResponse(
                COALESCE(SUM(CASE WHEN p.active = true
                    AND ps.stock > 0
                    AND (ps.stockMinimo <= 0 OR ps.stock >= ps.stockMinimo)
                    THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN p.active = false THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN p.active = true
                    AND (ps.stock = 0 OR (ps.stockMinimo > 0 AND ps.stock < ps.stockMinimo))
                    THEN 1L ELSE 0L END), 0L),
                COUNT(ps)
            )
            FROM ProductStock ps
            JOIN ps.producto p
            WHERE ps.sucursal.id = :storeId
            """)
    DashboardProductStatusResponse getProductStatusByStore(
            @Param("storeId") Long storeId
    );
}
