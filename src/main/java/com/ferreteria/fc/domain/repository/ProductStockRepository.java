
package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.persistence.entity.ProductStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductStockRepository
        extends JpaRepository<ProductStock, Long> {

    Optional<ProductStock> findByProductoIdAndSucursalId(
            Long productoId,
            Long sucursalId
    );

    List<ProductStock> findBySucursalId(Long sucursalId);

    List<ProductStock> findByProductoId(Long productoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT ps
        FROM ProductStock ps
        WHERE ps.producto.id = :productoId
          AND ps.sucursal.id = :sucursalId
    """)
    Optional<ProductStock> findByProductoAndSucursalForUpdate(
            @Param("productoId") Long productoId,
            @Param("sucursalId") Long sucursalId
    );
    @Query("""
    SELECT ps
    FROM ProductStock ps
    JOIN FETCH ps.producto
    WHERE ps.sucursal.id = :sucursalId
    """)
    List<ProductStock> findAllBySucursalIdWithProduct(
            @Param("sucursalId") Long sucursalId
    );

    @Query("""
    SELECT ps
    FROM ProductStock ps
    JOIN FETCH ps.producto p
    WHERE ps.sucursal.id = :sucursalId
      AND p.active = true
      AND (
          LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%'))
          OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
      )
    ORDER BY p.name ASC
""")
    List<ProductStock> searchBySucursalAndProduct(
            @Param("sucursalId") Long sucursalId,
            @Param("query") String query
    );
}
