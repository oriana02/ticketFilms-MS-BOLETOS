# PRD — TicketFilms

Plataforma cloud native de venta de entradas para cine y otros eventos.
Proyecto de curso (Evaluación Parcial 2, DSY1107).

Fecha del documento: 10 de octubre de 2026

---

## 1. Resumen

TicketFilms permite que una persona vea la cartelera de películas y eventos, elija sus asientos, los reserve por unos minutos y compre sus entradas. Después puede ver su historial de boletos con un código público (por ejemplo `TF-AB12-CD34`).

El sistema está dividido en microservicios de Spring Boot, con un API Manager (Spring Cloud Gateway) en la entrada, autenticación con Amazon Cognito (Google como proveedor de identidad) y una base de datos MySQL en Aiven. El frontend es React y está en Vercel.

## 2. Objetivos

1. Cumplir la rúbrica del curso y demostrar patrones cloud native funcionando de punta a punta.
2. Que dos personas nunca puedan comprar el mismo asiento.
3. Que el sistema no se limite al cine: debe soportar conciertos, deportes, teatro, ferias y eventos corporativos (retroalimentación del profesor).
4. Que cada servicio sea independiente y se comunique con los demás sin acoplarse (sin claves foráneas entre servicios).

### Fuera de alcance (por ahora)

- Pagos reales (el pago no está integrado).
- Reembolsos y anulaciones por parte del cliente.
- Panel de administración para crear eventos.
- Descubrimiento de servicios (Eureka/Consul): las URLs se configuran por propiedades.

## 3. Usuarios

| Usuario | Qué necesita |
|---|---|
| Cliente | Ver cartelera, filtrar por ciudad/fecha/tipo, elegir asientos, comprar, ver sus boletos |
| Equipo y profesor | Ver la arquitectura funcionando en la nube y probar el flujo completo |

## 4. Arquitectura

```
React (Vercel)
   │  JWT de Cognito en cada llamada
   ▼
API Manager (Spring Cloud Gateway, EC2)   ← valida JWT, rutas, CORS
   ├── ms-cartelera  (películas, eventos, funciones)
   ├── ms-asientos   (salas, mapa de asientos, reservas)
   └── ms-boletos    (compra, boletos, historial)
            │
            └── llama directo a ms-asientos para confirmar la compra
                (no pasa por el gateway)

Base de datos: Aiven for MySQL (db_cartelera, db_asientos, db_boletos)
Autenticación: Amazon Cognito + Google (Authorization Code + PKCE)
```

### Decisiones de diseño

- **Referencias lógicas entre servicios.** `funcion_id`, `evento_id`, `asiento_id` son números simples, sin clave foránea real entre bases de datos.
- **Reservar es síncrono y avisar es asíncrono.** La reserva temporal (RESERVADO con vencimiento) se hace por REST con bloqueo en base de datos. La mensajería (Kafka) solo se usa para notificar hechos ya ocurridos.
- **Comunicación entre servicios directa**, no a través del gateway.
- **El usuario siempre sale del JWT**, nunca del cuerpo de la solicitud.
- **Los datos del boleto son una copia** (título, fecha, asiento, precio) para que el historial no cambie si cartelera o asientos cambian después.

## 5. Estado actual por componente

### 5.1 Frontend (React + Vite, Vercel)

Implementado:
- Home, Películas, Eventos, Detalle, Funciones y Asientos (mapa interactivo).
- Login y logout con Cognito (react-oidc-context), rutas protegidas con `RequireAuth`.
- Capa HTTP con axios y JWT automático (`carteleraService`, `asientosService`, `boletosService`).
- La interfaz solo muestra los campos que el backend realmente entrega.

Pendiente:
- Pasar `Seats.jsx` a usar el mapa real de ms-asientos.
- Que el cliente indique cuántos asientos comprará y pueda elegir exactamente esa cantidad.
- Mostrar en el detalle: evento (sala, ciudad, duración, fecha próxima, categoría) y película (ciudad, género, director, reparto, duración, idioma, origen). El profesor lo va a revisar.
- Que las salas del footer y de los horarios vengan de la base de datos.
- Filtro por región o ciudad, además de fecha y tipo de evento.
- Enviar `tipoEvento`, `sede`, `ciudad` y `puerta` al comprar.
- Ajustar los precios al nuevo valor (5.500 estándar y 7.500 premium).

### 5.2 API Manager (Spring Cloud Gateway, EC2)

Implementado:
- Resource Server reactivo con rutas declarativas y CORS global.
- Probado en local: sin token responde 401 y las rutas públicas pasan.
- `issuer-uri` resuelto.

Pendiente:
- Validación de audiencia (RF-14).
- CORS para el origen de Vercel, incluidos los previews (RF-15).
- Despliegue en EC2 y evidencia de rutas, CORS y JWT aceptado/rechazado.

### 5.3 ms-cartelera (hecho por una compañera)

Implementado:
- Base de datos y capa de servicio; endpoints de películas, eventos y funciones.
- Género y director agregados.

Pendiente (retroalimentación del profesor):
- Entidades `Region` y `Sede`, y sede asociada a cada función.
- Columna `tipo` en evento (PELICULA, CONCIERTO, DEPORTE, TEATRO, FERIA, CORPORATIVO).
- Filtros por ciudad, región, tipo y fecha en `GET /funciones`.
- Usar `FuncionDTO` en el controlador (hoy devuelve la entidad cruda).
- Unificar las rutas `/funcion/{id}` y `/funciones`.

### 5.4 ms-asientos (hecho por una compañera)

Implementado:
- Mapa de asientos por función, generado la primera vez que se consulta (toma la sala desde cartelera con FeignClient).
- Reserva con vencimiento y liberación al consultar el mapa.
- `PUT /api/asientos/confirmar`: transaccional, con bloqueo (`SELECT FOR UPDATE`), valida existencia, vencimiento, estado RESERVADO y dueño de la reserva.
- Usuario tomado del JWT; manejo de errores (404 y 503).
- Sala SALA-02 (8 filas × 10 asientos, filas G y H premium).

Pendiente:
- Reservar de forma atómica para que dos usuarios no tomen el mismo asiento.
- `DELETE /api/asientos/reserva` (reemplaza el stub) y eliminar `GET /reserva/{id}`.
- Sectores NUMERADO y GENERAL (aforo) y precio por sector (resuelve el precio premium).
- Endpoint para confirmar entradas de sectores generales (descontar del aforo).
- Publicar `reserva.expirada` en Kafka.

### 5.5 ms-boletos (Oriana)

Implementado:
- Endpoints: `POST /api/boletos/compra`, `GET /api/boletos/historial`, `GET /api/boletos/{codigo}`.
- Seguridad: Resource Server con JWT, sesión sin estado, registro de 401 y 403.
- Flujo de compra en tres pasos: guardar boleto PENDIENTE, confirmar asientos en ms-asientos, marcar CONFIRMADO. Si falla el paso 2, el boleto queda CANCELADO.
- Código público único (`TF-XXXX-XXXX`) sin caracteres confusos.
- Un usuario solo ve sus propios boletos; si no existe o es de otro, responde 404.
- Manejo de errores: 400, 403, 404, 409, 410 y 503.
- Probado en local: compra y generación de boleto funcionan.

Cambios por la retroalimentación del profesor (hechos en código, falta compilar y probar):
- `boleto`: columnas `tipo_evento`, `sede`, `ciudad`, `puerta` (opcionales).
- `boleto_asiento`: columnas `sector`, `tipo_acceso` (GENERAL o VIP).
- Tabla nueva `boleto_entrada_general` para sectores sin asiento (ej. "Cancha").
- Validación de precios por categoría (estándar 5.500, premium 7.500) desde `application.properties`.

Pendiente:
- Confirmar entradas generales en ms-asientos (hoy se guardan, pero no descuentan aforo).
- Publicar `boleto.comprado` y `compra.fallida` en Kafka.
- Tomar el precio desde ms-asientos y eliminar la validación temporal.

### 5.6 Mensajería asíncrona (Kafka)

Pendiente de implementar. Eventos elegidos:

| Evento | Productor | Consumidor | Para qué |
|---|---|---|---|
| `boleto.comprado` | ms-boletos | ms-notificaciones (correo, QR) y ms-asientos | Avisar la compra sin bloquearla |
| `reserva.expirada` | ms-asientos | ms-asientos / interesados | Liberar reservas vencidas |
| `compra.fallida` | ms-boletos | ms-asientos | Liberar asientos si la compra falla |

Puntos que cuidar: patrón Outbox (guardar el evento en la misma transacción), idempotencia en los consumidores, topic de errores (DLQ), clave de partición por `funcionId` o `asientoId`, y mencionar como limitación que un solo broker es un punto único de falla.

### 5.7 Infraestructura

Implementado:
- Todo funciona en local sin gateway.
- Amazon Cognito con Google, página de login propia.

Pendiente:
- Pasar los microservicios a EC2 y configurar bases de datos en la nube.
- Recuperar Aiven (se cayó) y reprobar el flujo completo con el API Manager.
- IP elástica en EC2 y SSL/TLS en las conexiones a Aiven.
- Capturas y evidencia para la rúbrica.

## 6. Requerimientos

### 6.1 Funcionales

| Requerimiento | Estado |
|---|---|
| Ver cartelera de películas y eventos | Hecho |
| Ver funciones y horarios | Hecho |
| RF-06: filtrar funciones por fecha o tipo de evento | Parcial; falta ciudad y región |
| Ver mapa de asientos de una función | Hecho en backend; falta conectar el frontend |
| Reservar asientos con vencimiento (5 min) | Hecho; falta hacerlo atómico |
| Comprar y generar boleto con código público | Hecho |
| Ver historial de boletos del usuario | Hecho |
| Elegir la cantidad exacta de asientos | Pendiente |
| Sectores de admisión general (aforo) | Pendiente |
| Notificación por correo con QR | Pendiente |
| Soporte de múltiples tipos de evento | Pendiente |

### 6.2 No funcionales

| Requerimiento | Estado |
|---|---|
| RF-14: validación de audiencia del JWT | Pendiente |
| RF-15: CORS para el origen de Vercel | Pendiente |
| RNF-02: monitoreo de disponibilidad (`/actuator/health`) | Hecho |
| Autenticación OIDC con PKCE | Hecho |
| Aceptar y rechazar JWT en el gateway | Hecho en local; falta en la nube |
| Front y back desplegados | Pendiente (backend) |

## 7. Modelo de datos (resumen)

**db_boletos**
- `boleto`: id, codigo_boleto (único), usuario_id, funcion_id, evento_id, titulo_evento, fecha_hora_funcion, tipo_evento, sede, ciudad, puerta, precio_total, cantidad_asientos, estado (PENDIENTE, CONFIRMADO, CANCELADO), fecha_compra.
- `boleto_asiento`: id, boleto_id, asiento_id, fila, numero, categoria (ESTANDAR, PREMIUM), precio_pagado, sector, tipo_acceso.
- `boleto_entrada_general`: id, boleto_id, sector_id, sector, tipo_acceso, cantidad, precio_unitario.

**db_asientos** (actual): salas, asientos y `funcion_asiento` (estado DISPONIBLE, RESERVADO u OCUPADO, usuario y vencimiento de la reserva).
Propuesto: `sector` (NUMERADO o GENERAL, capacidad), `funcion_sector` (precio, vendidas) y `reserva_general`.

**db_cartelera** (actual): eventos y funciones.
Propuesto: `region`, `sede`, `funcion.sede_id` y `evento.tipo`.

## 8. Precios

| Categoría | Precio |
|---|---|
| Estándar / general | $5.500 |
| Premium (VIP) | $7.500 |

Mientras ms-asientos no entregue el precio por sector, ms-boletos valida estos valores desde su configuración.

## 9. Pendientes priorizados

1. **Desbloquear la entrega:** recuperar Aiven y pasar los servicios a EC2 con sus bases en la nube.
2. **Gateway:** audiencia (RF-14) y CORS de Vercel (RF-15); probar el flujo completo por el gateway.
3. **Retroalimentación del profesor:** sede, ciudad y región; filtro por ciudad; columna de tipo de evento.
4. **Frontend:** mapa real de asientos, cantidad exacta de asientos, campos de detalle y salas desde la base de datos.
5. **Kafka:** `boleto.comprado` y `reserva.expirada` (con Outbox e idempotencia).
6. **Sectores generales y precio por sector** en ms-asientos, y su confirmación desde ms-boletos.
7. **Evidencia:** capturas, actualización del informe, pruebas P-01 a P-07 más las nuevas (filtro por ciudad).

## 10. Riesgos

| Riesgo | Qué hacer |
|---|---|
| Aiven caído o sin acceso | Tener un plan B (MySQL en la misma EC2 o RDS) |
| Precio enviado por el frontend | Validación temporal; luego tomarlo de ms-asientos |
| Compra que queda PENDIENTE si falla el último paso | Es detectable; documentarlo y, si hay tiempo, un proceso que corrija |
| Un solo broker Kafka en EC2 (memoria y punto único de falla) | Declararlo como limitación o evaluar SNS/SQS |
| Desfase de tiempos entre las dos compañeras y Oriana | Acordar los contratos de endpoints antes de integrar |
| Resultados de pruebas marcados "Exitoso" en el Anexo D mientras el informe refleja estados corregidos | Unificar antes de entregar |

## 11. Entregables

- Informe ejecutivo.
- Anexo A: diagrama lógico (draw.io).
- Anexo D: plan de pruebas (P-01 a P-07).
- Presentación de 5 a 10 minutos con evidencia de API Manager, rutas, CORS, JWT, tenant con usuarios, app registrada, OIDC con PKCE, y front y back desplegados.
