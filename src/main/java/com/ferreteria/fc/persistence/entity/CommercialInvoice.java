package com.ferreteria.fc.persistence.entity;

import com.ferreteria.fc.domain.enums.CommercialInvoiceStatus;
import com.ferreteria.fc.domain.enums.InvoiceTaxStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "commercial_invoices", indexes = {
        @Index(name = "idx_commercial_invoice_store_created", columnList = "store_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommercialInvoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "internal_number", unique = true, length = 30)
    private String internalNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_order_id", unique = true)
    private Order sourceOrder;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "customer_rut", length = 12)
    private String customerRut;

    @Column(name = "customer_business_name", length = 200)
    private String customerBusinessName;

    @Column(name = "customer_giro", length = 150)
    private String customerGiro;

    @Column(name = "customer_address", length = 250)
    private String customerAddress;

    @Column(name = "customer_city", length = 150)
    private String customerCity;

    @Column(name = "customer_commune", length = 150)
    private String customerCommune;

    @Column(name = "customer_email", length = 150)
    private String customerEmail;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CommercialInvoiceStatus status = CommercialInvoiceStatus.REGISTERED;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_status", nullable = false, length = 20)
    @Builder.Default
    private InvoiceTaxStatus taxStatus = InvoiceTaxStatus.NOT_SENT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommercialInvoiceItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;
}
