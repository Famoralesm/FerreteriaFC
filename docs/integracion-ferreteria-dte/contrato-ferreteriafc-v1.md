# Contrato de integración futura: FerreteríaFC → Ferretería DTE

**Estado:** borrador técnico v1. No existe conexión activa con el SII ni se realiza envío tributario desde FerreteríaFC.

## 1. Responsabilidades

- **FerreteríaFC** mantiene el registro comercial, el pedido de origen, el cliente, la sucursal, las líneas y los precios comerciales históricos.
- **Ferretería DTE** valida la configuración tributaria del contribuyente, determina el tipo de DTE autorizado, aplica reglas tributarias confirmadas, genera XML/TED, firma, administra CAF/folios, envía al SII y consulta el resultado.
- FerreteríaFC no almacena certificados privados, claves del certificado ni archivos CAF.

## 2. Identificación e idempotencia

Cada registro comercial se identifica por `invoiceId` (ID interno de FerreteríaFC). El número `internalNumber` (formato FC-00000001) es exclusivamente interno y **no es un folio tributario**.

La solicitud a DTE deberá incluir una clave idempotente estable:

```
ferreteriafc:commercial-invoice:{invoiceId}
```

Reintentar la misma solicitud no debe generar un segundo DTE.

## 3. Operación propuesta

Cuando exista integración, FerreteríaFC invocará:

```
POST /api/v1/documents
Authorization: Bearer <token de integración>
Idempotency-Key: ferreteriafc:commercial-invoice:{invoiceId}
Content-Type: application/json
```

El DTE responderá con un identificador de operación, estado de recepción y versión de contrato. La aceptación técnica de la solicitud no equivale a aceptación del SII.

FerreteríaFC consultará posteriormente:

```
GET /api/v1/documents/{dteOperationId}
```

Los nombres y autenticación definitivos deberán acordarse al crear el repositorio Ferretería-DTE.

## 4. Estructura de datos propuesta

```json
{
  "contractVersion": "1.0",
  "source": {
    "system": "FERRETERIA_FC",
    "documentType": "COMMERCIAL_INVOICE",
    "documentId": 125,
    "internalNumber": "FC-00000125",
    "sourceOrderId": 84,
    "sourceOrderNumber": "A0084",
    "createdAt": "2026-11-28T10:30:00"
  },
  "issuer": {
    "companyId": null,
    "storeId": 2
  },
  "receiver": {
    "rut": null,
    "businessName": null,
    "name": "Cliente",
    "businessActivity": null,
    "address": null,
    "city": null,
    "commune": null,
    "email": null,
    "phone": null
  },
  "lines": [
    {
      "productId": 10,
      "productName": "Producto",
      "quantity": 2,
      "unitName": "Unidad",
      "unitPrice": 1000,
      "lineTotal": 2000
    }
  ],
  "commercialTotals": {
    "total": 2000,
    "currency": "CLP"
  }
}
```

Los valores nulos indican datos que FerreteríaFC todavía no tiene o cuya captura tributaria debe definirse. El contrato de producción deberá incorporar SKU, unidad tributaria, clasificación afecta/exenta, precios netos/brutos y desglose de impuestos cuando Alexis y el contador confirmen esas reglas.

## 5. Estados separados

### Estado comercial en FerreteríaFC

- `REGISTERED`: registro comercial creado.
- `CANCELED`: registro comercial anulado antes de iniciar la gestión tributaria.

### Estado tributario reservado para DTE

- `NOT_SENT`: todavía no se ha enviado a Ferretería DTE.
- `PENDING`: operación recibida/en proceso.
- `ACCEPTED`: documento aceptado por el SII.
- `REJECTED`: documento rechazado por el SII.

`REGISTERED` nunca significa «factura emitida ante el SII». La interfaz debe mostrar explícitamente «No enviada al DTE» mientras el estado tributario sea `NOT_SENT`.

## 6. Reglas de seguridad e integridad

1. El ID de FerreteríaFC y la clave idempotente deben ser únicos por sistema origen.
2. La sucursal debe resolverse contra la empresa/RUT emisor configurado en DTE; no confiar en un RUT enviado libremente desde el frontend.
3. Los documentos deben quedar aislados por contribuyente.
4. Los estados tributarios solo podrán actualizarse con respuestas verificables del servicio DTE.
5. No se debe permitir anular localmente un registro cuando el envío tributario ya comenzó; en ese caso se requerirá el flujo tributario que defina DTE.
6. Conservar los datos comerciales originales y el vínculo con el pedido de origen.

## 7. Pendientes antes de activar el contrato

- RUT/razón social emisora y relación entre sucursales y contribuyentes.
- Tipo de documento tributario aplicable (factura afecta código 33 u otro).
- Régimen tributario, afectación/exención y reglas de precios con IVA.
- Campos obligatorios del receptor y reglas de identificación.
- Certificado digital, CAF y habilitación del contribuyente.
- Política de notas de crédito, anulaciones y documentos relacionados.
- Autenticación entre sistemas, secretos, TLS, reintentos y notificaciones.

Hasta cerrar estos puntos, la API de FerreteríaFC solo registra información comercial y mantiene `taxStatus = NOT_SENT`.
