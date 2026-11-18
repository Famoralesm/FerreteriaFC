
package com.ferreteria.fc.domain.enums;

public enum MovementType {
    INITIAL,      // Stock inicial al crear o migrar un producto
    ENTRY,        // Ingreso de mercadería
    EXIT,         // Salida por venta o pedido
    ADJUSTMENT,   // Corrección por diferencia, pérdida o daño
    RETURN        // Devolución o reversa de una salida
}
