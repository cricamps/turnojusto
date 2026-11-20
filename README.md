# TurnoJusto

Plataforma cloud native para la **gestión de empleados y turnos rotativos equitativos** en empresas con una o más sucursales.

Proyecto de la asignatura **Taller Aplicado de Software (TSY2201)**, Duoc UC · Grupo 2: Cynthia Torres y Cristobal Camps · Docente: Alonso Esteban Castillo Pizarro.

> **Sistema en línea:** https://turnojusto.xkcx.cl · **API:** Amazon API Gateway (ver la salida del workflow *Despliegue QA*)
>
> **Estado: Sprint 1 (semana 5) · versión `v0.1.0-sprint1`.** Hay dos microservicios desplegados y el frontend está publicado. El detalle de lo implementado está en [docs/AVANCE_SPRINT1.md](docs/AVANCE_SPRINT1.md).

---

## Arquitectura multinube

```
                     ┌────────────────────────── Microsoft Azure ─────────────────────────┐
                     │  Microsoft Entra (IDaaS): login OIDC + PKCE, MFA, tokens JWT v2    │
                     │  Microsoft Graph: invitación automática de las cuentas nuevas      │
                     └──────────────▲───────────────────────────────▲────────────────────┘
                                    │ token                         │ invitación
┌──────────────────────────── Amazon Web Services ──────────────────┼───────────────────────┐
│                                                                    │                       │
│  CloudFront + S3 ──► SPA React (MSAL)   (copia de revisión en turnojusto.xkcx.cl, cPanel) │
│        │ HTTPS + Bearer JWT                                        │                       │
│        ▼                                                           │                       │
│  API Gateway (HTTP API) ── autorizador JWT de Entra ── VPC Link    │                       │
│        │                         Cloud Map (turnojusto.local)      │                       │
│        ├──► ECS Fargate · ms-organizacion ◄──── REST ────► ECS Fargate · ms-empleados      │
│        │       (empresa, sucursales, equipos)     (fichas, traslados, desactivación)       │
│  ECR (imágenes) · Secrets Manager (claves) · CloudWatch Logs                               │
└────────────────┬───────────────────────────────────────────────────┬───────────────────────┘
                 │ TLS 1.2 (JDBC)                                    │
┌────────────────▼──────────── Oracle Cloud Infrastructure ──────────▼───────────────────────┐
│  Autonomous Database (Always Free, Santiago) · un esquema por microservicio:                │
│  ORG · EMP · TUR · ASG · REP  (usuario dueño para Flyway + usuario <ESQ>_APP con solo DML)  │
└────────────────────────────────────────────────────────────────────────────────────────────┘
```

| Patrón | Cómo se aplica |
|---|---|
| Microservicios + base de datos por servicio | `ms-organizacion` es dueño del esquema ORG y `ms-empleados` del esquema EMP. No hay llaves foráneas entre esquemas. |
| API Gateway | Un solo punto de entrada valida el JWT (emisor, audiencia y vigencia) antes de enrutar. |
| Descubrimiento de servicios | Los servicios se llaman por `http://<servicio>.turnojusto.local:8080` (AWS Cloud Map). |
| Token relay | Un servicio reenvía el token del usuario al llamar a otro, para que cada uno aplique sus propias reglas. |
| Outbox | Los eventos (`EmpleadoTrasladado`, `EmpleadoDesactivado`, etc.) se guardan en la misma transacción que el cambio. |
| Multiempresa | Cada consulta se filtra por `EMPRESA_ID`. El Coordinador solo ve su sucursal y el Empleado solo su propia ficha. |
| IDaaS | Entra autentica a la persona. Su ficha, su rol y su empresa se guardan en Oracle (`EMP.EMPLEADO`). |

## Estructura del repositorio

```
turnojusto/
├── services/
│   ├── ms-organizacion/   Java 21 · Spring Boot 4 · empresa, sucursales, equipos y coordinadores (F2, F4)
│   └── ms-empleados/      Java 21 · Spring Boot 4 · fichas, traslados, desactivación y contexto (F1, F3)
├── frontend/              React 19 · TypeScript · Vite · MUI · TanStack Query · MSAL (pantallas por rol)
├── db/                    Migraciones Flyway por esquema (V1 de la semana 4 + mejoras v2 + semilla QA)
├── infra/aws/             Terraform: ECR, ECS Fargate, Cloud Map, API Gateway, Secrets Manager, S3 y CloudFront
├── docs/                  Avance del sprint, cambios del modelo, mockups v1 y v2
└── .github/workflows/     CI (build, pruebas y cobertura) y despliegue continuo a QA
```

Cada carpeta `services/*` es un proyecto independiente, con su propio `pom.xml` y `Dockerfile`, y se construye y despliega por separado.

## Funcionalidades del Sprint 1

| # | Funcionalidad | Estado |
|---|---|---|
| F1 | Acceso seguro con Entra, roles y aislamiento por empresa y sucursal | Implementada |
| F2 | Datos de la empresa y registro de sucursales (dirección, comuna, región y horario) | Implementada |
| F3 | Gestión de empleados: ficha, búsqueda con filtros, traslados con historial, desactivación y reactivación | Implementada |
| F4 | Equipos por sucursal y asignación de coordinador con historial | Implementada |
| F5 a F11 | Turnos, feriados, disponibilidad, asignación equitativa, calendario, reemplazos y reportes | Sprints 2 y 3 |

## API

| Servicio | Recurso | Operaciones |
|---|---|---|
| ms-organizacion | `/api/v1/empresa` | `GET`, `PUT` |
| | `/api/v1/regiones` | `GET` |
| | `/api/v1/sucursales` | `GET`, `POST`, `GET/PUT /{id}`, `POST /{id}/activar` y `POST /{id}/desactivar` |
| | `/api/v1/equipos` | `GET`, `POST`, `GET/PUT /{id}`, `PUT /{id}/coordinador`, `GET /{id}/coordinadores`, `POST /{id}/activar` y `POST /{id}/desactivar` |
| ms-empleados | `/api/v1/empleados` | `GET` (búsqueda paginada), `POST`, `GET /me`, `GET /resumen`, `GET/PUT /{id}` |
| | `/api/v1/empleados/{id}/traslados` | `GET`, `POST` |
| | `/api/v1/empleados/{id}/desactivar` y `/{id}/reactivar` | `POST` |

El contrato OpenAPI 3 de cada servicio se publica en `<API>/organizacion/v3/api-docs` y en `<API>/empleados/v3/api-docs`. Los errores siguen el formato RFC 9457 (`application/problem+json`) y traen el mensaje en español.

## Base de datos

- `db/V0__usuarios_por_servicio.sql`: usuarios por servicio. Se ejecuta como ADMIN.
- `db/org/V1`, `db/emp/V1`, …: modelo físico de la semana 4, con 5 esquemas y 23 tablas.
- `db/org/V3__mejoras_sprint1.sql` y `db/emp/V2__mejoras_sprint1.sql`: **mejoras del modelo (v2)**. El detalle está en [docs/CAMBIOS_MODELO_DATOS.md](docs/CAMBIOS_MODELO_DATOS.md).
- `db/org/V4__datos_semilla_qa.sql` y `db/emp/V3__datos_semilla_qa.sql`: datos ficticios para QA: empresa *Comercial Los Andes SpA*, 3 sucursales, 5 equipos y 18 fichas.

## Integración y despliegue continuo

| Workflow | Cuándo corre | Qué hace |
|---|---|---|
| `CI` | En cada push y pull request | Corre `mvn verify` en cada microservicio (pruebas JUnit 5 + Mockito y cobertura JaCoCo), luego Vitest y el build del frontend, y al final `terraform validate`. |
| `Despliegue QA` | En cada push a `main`, o de forma manual | Aplica Terraform, construye y publica las imágenes en ECR con el SHA del commit, actualiza ECS, publica la SPA en S3 e invalida CloudFront. |

### Configuración del repositorio (Settings → Secrets and variables → Actions)

| Tipo | Nombre | Valor |
|---|---|---|
| Secreto | `AWS_CREDENCIALES` | Bloque `[default]` de *AWS Details → AWS CLI* del Learner Lab. Hay que renovarlo en cada sesión del lab. |
| Secreto | `TJ_DB_CLAVE_ORG` / `TJ_DB_CLAVE_EMP` | Contraseñas de `ORG_APP` y `EMP_APP` en Oracle. |
| Secreto | `ENTRA_SECRETO` | Secreto de cliente de la app TurnoJusto en Entra (Microsoft Graph). |
| Variable | `TJ_DB_URL` | URL JDBC TLS de la Autonomous Database. |
| Variable | `ENTRA_TENANT_ID`, `ENTRA_CLIENT_ID`, `ENTRA_SCOPE` | Datos del registro de la app en Entra. |

> En AWS Academy los contenedores de ECS se detienen al terminar la sesión del Learner Lab y vuelven a iniciarse en la sesión siguiente. Para revisar el sistema hay que iniciar el lab y, si las credenciales cambiaron, actualizar `AWS_CREDENCIALES` y volver a ejecutar *Despliegue QA*.

## Desarrollo local

```bash
# Microservicios (requiere Java 21 y Maven)
cd services/ms-empleados && mvn verify
# Frontend
cd frontend && cp .env.example .env.local && npm install && npm run dev
```

## Equipo y metodología

Usamos Scrum con sprints semanales. Cristobal Camps es líder técnico y Scrum Master, y Cynthia Torres es Product Owner y líder de calidad. Cada sprint se integra en `main`, pasa el CI y queda etiquetado (`v0.1.0-sprint1`, …).
