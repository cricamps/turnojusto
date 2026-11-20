# Avance del Sprint 1 · Semana 5 (16-11 al 22-11-2026)

**Objetivo del sprint:** dejar operativo el acceso seguro por rol y la administración de la organización (empresa, sucursales y equipos) y de los empleados, desplegados como microservicios independientes en la nube.

## Funcionalidades implementadas y desplegadas

| # | Funcionalidad | Qué se puede hacer hoy en el sistema |
|---|---|---|
| F1 | Acceso seguro y multiempresa | Iniciar sesión con Microsoft Entra (OIDC + PKCE, con MFA según la política del inquilino). El API Gateway rechaza las solicitudes sin un token válido. Cada servicio valida el token, obtiene la empresa, la sucursal y el rol desde la ficha en Oracle y filtra todos los datos por empresa. El menú y las acciones cambian según el rol. |
| F2 | Empresa y sucursales | Ver y editar los datos de la empresa. Registrar, editar, activar y desactivar sucursales con dirección, comuna, región y horario. Se valida que el horario sea coherente, que el código sea único y que la región exista; además, una sucursal con equipos activos no se puede desactivar. |
| F3 | Gestión de empleados | Buscar por nombre, RUT o correo con filtros por sucursal, equipo y estado, con paginación. Registrar y editar fichas, validando el dígito verificador del RUT y que el RUT y el correo no se repitan. Al registrar a alguien se le crea automáticamente una cuenta en Entra (invitación). También se puede trasladar entre sucursales y equipos (con historial y el evento `EmpleadoTrasladado`), desactivar con fecha y motivo, y reactivar. |
| F4 | Equipos y coordinadores | Crear y editar equipos por sucursal sin nombres repetidos, y activarlos o desactivarlos. Asignar un coordinador, que debe pertenecer a la misma sucursal y estar activo con rol Coordinador o Administrador; el coordinador anterior queda en el historial. |

### Visibilidad por rol

| Rol | Ve | Puede modificar |
|---|---|---|
| Administrador | Toda la empresa | Empresa, sucursales, equipos, coordinadores y empleados |
| Coordinador | Su sucursal: equipos y personas | Nada en este sprint; en el Sprint 2 gestiona los turnos |
| Empleado | Solo su ficha (*Mi ficha*) | Nada en este sprint |

## Estado de la arquitectura de microservicios

| Componente | Nube | Despliegue | Estado |
|---|---|---|---|
| SPA React | Hosting HTTPS `turnojusto.xkcx.cl` (el Learner Lab bloquea CloudFront) | El pipeline la publica en la rama `web` y el cPanel la descarga cada 5 minutos | Operativa |
| API Gateway (HTTP API + autorizador JWT) | AWS | Terraform | Operativo |
| ms-organizacion | AWS ECS Fargate (contenedor propio, 0,5 vCPU y 1 GB) | Imagen en ECR, servicio ECS independiente | Operativo |
| ms-empleados | AWS ECS Fargate (contenedor propio) | Imagen en ECR, servicio ECS independiente | Operativo |
| Descubrimiento y balanceo | ALB interno de AWS, un listener por microservicio (el Learner Lab bloquea Cloud Map) | Terraform | Operativo |
| Identidad (IDaaS) | Microsoft Entra (Azure) | App TurnoJusto: scope `access_as_user`, 3 app roles y tokens v2 | Operativa |
| Base de datos | Oracle Autonomous Database (OCI, Santiago) | Esquemas ORG y EMP en v2, más la semilla QA | Operativa |
| ms-turnos, ms-asignacion, ms-reportes, fn-notificaciones y Service Bus | AWS y Azure | Sprints 2 y 3 | Pendiente |

Cada microservicio se construye, se publica y se despliega por separado: tiene su propio `Dockerfile`, repositorio ECR, definición de tarea, servicio ECS, grupo de logs y usuario de base de datos. La salud de cada uno se puede consultar públicamente en `<API>/organizacion/actuator/health` y en `<API>/empleados/actuator/health`.

## Calidad

- **Pruebas unitarias:** ms-empleados tiene 51 y ms-organizacion 36 (JUnit 5, Mockito, MockMvc con JWT simulado y MockRestServiceServer). El frontend tiene 6 (Vitest). Todas corren en el CI en cada push.
- **Cobertura de líneas:** 81 % en ms-empleados y 62 % en ms-organizacion. Los servicios de negocio superan el 85 %.
- **Casos del plan de pruebas cubiertos por pruebas automatizadas:** CP-01, CP-02, CP-03, CP-13, CP-14, CP-15, CP-17, CP-18, CP-19, CP-20, CP-21, CP-22 y CP-26.

## Pendiente para el Sprint 2

Configuración de turnos (F5), feriados y disponibilidad (F6 y F7), y el algoritmo de asignación equitativa (F8). También publicar los eventos del outbox en Azure Service Bus y automatizar con Newman las pruebas de permisos que hoy se prueban en el código.
