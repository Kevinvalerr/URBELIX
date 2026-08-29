# Integracion Wompi Sandbox

Urbelix usa el Checkout Web de Wompi para iniciar pagos PSE y tarjeta y procesa el evento
`transaction.updated` mediante un webhook firmado. Las llaves privadas y los
secretos se leen solamente desde variables de entorno.

## Variables locales

En Windows se pueden definir para el usuario actual desde PowerShell:

```powershell
[Environment]::SetEnvironmentVariable("WOMPI_BASE_URL", "https://sandbox.wompi.co/v1/", "User")
[Environment]::SetEnvironmentVariable("WOMPI_PUBLIC_KEY", "<clave-publica-de-prueba>", "User")
[Environment]::SetEnvironmentVariable("WOMPI_PRIVATE_KEY", "<clave-privada-de-prueba>", "User")
[Environment]::SetEnvironmentVariable("WOMPI_INTEGRITY_SECRET", "<secreto-de-integridad>", "User")
[Environment]::SetEnvironmentVariable("WOMPI_EVENTS_SECRET", "<secreto-de-eventos>", "User")
```

Tambien se puede usar un archivo de secretos local fuera de Git o un gestor de
secretos. No se deben guardar valores reales en `application.properties`, en
plantillas, en pruebas ni en capturas de pantalla.

El Checkout Web usa la llave publica, la referencia unica, el monto en centavos,
la moneda `COP` y la firma de integridad generada por el backend. La llave
privada no se envia al navegador.

## Sandbox local sin cobro

El perfil `dev` incluye un checkout local para validar pagos PSE y tarjeta sin
depender de Internet ni de credenciales Wompi. Se activa por defecto con
`PAYMENTS_SIMULATION_ENABLED=true`. Luego de crear un pago PSE o tarjeta, el residente
pulsa `Preparar checkout en línea`, abre `Checkout sandbox local` y selecciona
`APPROVED`, `PENDING`, `DECLINED`, `VOIDED` o `ERROR`.

La simulación no modifica el pago directamente: genera un evento
`transaction.updated` con una transacción, referencia, monto y checksum, y lo
envía al mismo procesador de eventos firmado que usa el webhook. Solo
`APPROVED` marca el pago como `PAGADO`; los demás resultados se registran y lo
mantienen pendiente. El sandbox usa `PAYMENTS_SIMULATION_SECRET`, separado de
`WOMPI_EVENTS_SECRET`. En `prod` esta opción está desactivada por defecto.

## Correo de recuperacion

Para Gmail se requiere una contrasena de aplicacion, no la contrasena normal de
la cuenta:

```powershell
[Environment]::SetEnvironmentVariable("MAIL_HOST", "smtp.gmail.com", "User")
[Environment]::SetEnvironmentVariable("MAIL_PORT", "587", "User")
[Environment]::SetEnvironmentVariable("MAIL_USERNAME", "tu-cuenta@gmail.com", "User")
[Environment]::SetEnvironmentVariable("MAIL_PASSWORD", "tu-contrasena-de-aplicacion", "User")
[Environment]::SetEnvironmentVariable("NOTIFICATIONS_EMAIL_ENABLED", "true", "User")
```

Despues de cambiar variables de usuario se debe abrir una nueva terminal antes
de ejecutar Maven.

## Webhook

El endpoint local es:

```text
POST /webhooks/wompi
```

Acepta el checksum en `X-Event-Checksum` y tambien el checksum dentro de
`signature.checksum`. Solo procesa eventos `transaction.updated`, valida monto y
referencia, marca como pagado un evento `APPROVED` y evita reprocesar el mismo
evento.

Para probar eventos reales, `APP_BASE_URL` debe ser una URL HTTPS publica que
apunte al equipo o al despliegue. Una URL como `http://localhost:8080` sirve
para el retorno del navegador, pero no para que Wompi alcance el webhook desde
Internet. La URL que se registra en Wompi debe terminar en:

```text
/webhooks/wompi
```

Se debe usar una URL distinta para sandbox y produccion. Antes de publicar el
sistema se deben cambiar las llaves de prueba por credenciales de produccion y
rotar cualquier secreto que haya sido compartido durante la configuracion.

## Ejecucion local

```powershell
.\mvnw.cmd spring-boot:run
```

Luego se abre `http://localhost:8080`, se inicia sesion con un residente, se
abre un pago PSE o tarjeta pendiente, se pulsa `Preparar checkout en línea` y finalmente
`Pagar en checkout Wompi`.

Para probar sin proveedor externo, después de preparar el pago se pulsa
`Checkout sandbox local` y se elige el resultado que se desea verificar.
