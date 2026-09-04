# Pagos simulados locales

URBELIX usa un flujo de pagos local para demostracion y pruebas. No se conecta a Wompi, PSE, bancos ni otra pasarela, y no requiere llaves, secretos, webhooks o credenciales externas.

## Flujo

1. El administrador crea la obligacion y la asigna al residente.
2. El residente prepara un pago PSE o tarjeta; el sistema genera una referencia interna.
3. El residente abre el simulador y elige un escenario.
4. Solo `APPROVED` cambia el pago a `PAGADO` y registra `fechaPago`.
5. `PENDING`, `DECLINED`, `VOIDED` y `ERROR` dejan la obligacion sin pagar.
6. Un pago aprobado permite consultar y descargar su factura PDF; todos los registros permanecen visibles para ADMIN.

Transferencia y efectivo no aparecen como pagos simulables para RESIDENTE. Se
mantienen como metodos administrativos y solo ADMIN puede confirmarlos desde el
detalle del pago. El backend aplica la misma regla aunque alguien intente llamar
directamente a una ruta.

## Configuracion

La simulacion esta activa por defecto en desarrollo mediante `app.payments.simulation-enabled=true`.
Para desactivarla se puede definir `PAYMENTS_SIMULATION_ENABLED=false`.

## Alcance

Este flujo demuestra estados, referencia, fecha efectiva, trazabilidad visual y factura sin riesgo financiero. No representa una transaccion bancaria real y no debe presentarse como integracion productiva.
