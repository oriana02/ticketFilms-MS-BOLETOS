ms-boletos

Microservicio de gestión de boletos para **TicketFilms**. Responsable de
confirmar compras, registrar boletos y exponer el historial de compras
del usuario autenticado.

Cubre los requerimientos: **RF-10, RF-11, RF-12, RF-14, RNF-01, RNF-06, RNF-07**.

---

## Arquitectura de capas

El servicio sigue una organización en capas limpias, con responsabilidades
separadas y sin lógica de negocio filtrada hacia capas que no le corresponden:
src/main/java/com/ticketfilms/ms_boletos/
├── controller/ # Expone los endpoints REST. Sin lógica de negocio:
│ solo recibe el request, extrae el usuario del JWT
│ y delega al service.
│
├── service/ # Lógica de negocio (confirmar compra, historial).
│ └── support/ # Componentes de apoyo aislados y testeables
│ (ej. generación de código de boleto).
│
├── repository/ # Acceso a datos vía Spring Data JPA. Sin lógica de
│ negocio, solo consultas.
│
├── model/ # Entidades JPA (@Entity), mapeadas 1:1 contra el
│ esquema de base de datos (ms-boletos-schema.sql).
│
├── dto/ # Objetos de transferencia entre el cliente y el
│ service. El modelo interno (model/) nunca se
│ expone directamente en la API.
│
├── client/ # Cliente HTTP hacia otros microservicios
│ └── dto/ (ms-asientos), vía RestClient.
│
├── config/ # Configuración de Spring: seguridad (Resource
│ Server) y beans de infraestructura (RestClient).
│
└── exception/ # Manejo centralizado de errores (@RestControllerAdvice),
traduce excepciones de negocio a códigos HTTP.


Esta separación existe para que un cambio en una capa (ej. cambiar de
MySQL a otra base de datos) no obligue a tocar el resto del código.

---

## Seguridad (RF-14, RNF-01, RNF-07)

`ms-boletos` actúa como **Resource Server** de OAuth2: no gestiona login,
solo valida el JWT emitido externamente por Google Auth Platform en cada
request (`Authorization: Bearer <token>`).

- Todas las rutas bajo `/api/boletos/**` exigen autenticación (RF-17).
- El `usuarioId` usado en la lógica de negocio se extrae siempre del claim
  `sub` del JWT ya validado — nunca se confía en un valor enviado en el
  body del request.
- Los rechazos de autenticación (401) y autorización (403) quedan
  registrados en los logs de la aplicación (RNF-07), antes de responder
  al cliente.

> Nota: por ahora `ms-boletos` valida el JWT directamente contra Google.
> Cuando el API Manager esté implementado, esta validación se mantiene
> igual (defensa en profundidad) — el gateway se suma como una capa
> adicional de filtrado antes de llegar al microservicio.

---

## Endpoints

| Método | Ruta | Descripción | Requiere JWT |
|---|---|---|---|
| `POST` | `/api/boletos/compra` | Confirma la compra de asientos reservados y genera el boleto (RF-10, RF-11) | Sí |
| `GET` | `/api/boletos/historial` | Historial de boletos del usuario autenticado (RF-12) | Sí |
| `GET` | `/api/boletos/{codigo}` | Consulta un boleto por su código público (pantalla de confirmación / QR) | Sí |

---

## Base de datos

Base de datos independiente `db_boletos` (MySQL), sin llaves foráneas
hacia otros microservicios — `funcion_id` y `evento_id` son referencias
lógicas a `ms-cartelera`, resueltas a nivel de aplicación.

---

## Alcance de esta entrega

Esta es una **primera versión funcional**, priorizando el flujo de
autenticación (login con IdaaS) y el API Manager como foco principal de
la evaluación. Queda pendiente:

- Conectar `AsientosClient` (ya implementado) al endpoint de `ms-asientos`
  que marque los asientos como `OCUPADO` tras confirmar la compra, una
  vez ese endpoint exista en `ms-asientos`.
- Integrar el API Manager como intermediario entre el frontend y este
  microservicio.
- Validar el claim `aud` del JWT de Google contra el Client ID real de
  la aplicación, una vez esté configurado el login real (por ahora se
  valida únicamente `issuer` y firma).

---

## Cómo correr localmente

```bash
mvn spring-boot:run
```
