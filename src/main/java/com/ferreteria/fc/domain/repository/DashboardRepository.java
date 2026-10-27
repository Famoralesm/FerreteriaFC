package com.ferreteria.fc.domain.repository;


import com.ferreteria.fc.persistence.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DashboardRepository extends JpaRepository<Order, Long> {

    @Query(value = """
            SELECT COALESCE(SUM(total),0)
            FROM orders
            WHERE DATE(created_at) = CURRENT_DATE
            """, nativeQuery = true)
    Double getVentasHoy();

    @Query(value = """
            SELECT COUNT(*)
            FROM orders
            WHERE status = 'PENDIENTE'
            """, nativeQuery = true)
    Long getPedidosPendientes();

    @Query(value = """
            SELECT COUNT(*)
            FROM customer
            """, nativeQuery = true)
    Long getClientes();

    @Query(value = """
            SELECT COUNT(*)
            FROM product
            """, nativeQuery = true)
    Long getProductos();

}