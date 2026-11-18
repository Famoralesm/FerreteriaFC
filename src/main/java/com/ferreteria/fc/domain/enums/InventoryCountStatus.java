
package com.ferreteria.fc.domain.enums;

public enum InventoryCountStatus {
    DRAFT,        // Conteo creado, aún no iniciado
    IN_PROGRESS,  // Conteo físico en ejecución
    COMPLETED,    // Conteo aplicado al stock
    CANCELED      // Conteo descartado
}
