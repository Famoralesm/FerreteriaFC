package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.domain.dto.response.OrderSummaryResponse;
import com.ferreteria.fc.persistence.entity.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("""
                SELECT new com.ferreteria.fc.domain.dto.response.OrderSummaryResponse(
                    o.id,
                    o.customerName,
                    o.createdAt,
                    o.totalAmount,
                    o.status,
                    o.statusChangedAt
                )
                FROM Order o
                ORDER BY o.createdAt DESC
                """)
    List<OrderSummaryResponse> findLatestOrders(Pageable pageable);
    @Query("""
                SELECT new com.ferreteria.fc.domain.dto.response.OrderSummaryResponse(
                    o.id,
                    o.customerName,
                    o.createdAt,
                    o.totalAmount,
                    o.status,
                    o.statusChangedAt
                )
                FROM Order o
                ORDER BY o.createdAt DESC
                """)
    List<OrderSummaryResponse> findAllOrders();


}