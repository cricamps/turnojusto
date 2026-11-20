# Cambios en el modelo de datos · v2 (Sprint 1)

El modelo físico de la semana 4 (versión 1) tenía 5 esquemas y 23 tablas. Al programar `ms-organizacion` y `ms-empleados` encontramos ajustes necesarios, que se aplican como migraciones nuevas sin modificar las anteriores. Así, cualquier ambiente se puede reconstruir en orden con Flyway.

| Migración | Esquema | Cambio | Motivo |
|---|---|---|---|
| `org/V3__mejoras_sprint1.sql` | ORG | FK `EQUIPO.EMPRESA_ID` → `EMPRESA` y FK `COORDINADOR_EQUIPO.EMPRESA_ID` → `EMPRESA` | En v1 la empresa del equipo solo se validaba en la API. Ahora la base también la asegura. |
| | ORG | Columna `VERSION` en `EMPRESA`, `SUCURSAL` y `EQUIPO` | Control de concurrencia optimista (`@Version`). Si dos administradores editan lo mismo a la vez, el segundo recibe 409 y no pisa el cambio del primero. |
| | ORG | `CHECK` del formato del RUT (`76123456-0`) y del correo de contacto | El RUT se guarda normalizado (sin puntos). El dígito verificador lo valida la API. |
| | ORG | Índice único por función `UX_COOEQU_VIGENTE` | Asegura que cada equipo tenga **un solo coordinador vigente** (`VIGENTE_HASTA` nulo). |
| | ORG | Índices `IX_SUC_EMPRESA_ESTADO` y `IX_EQU_EMPRESA_SUCURSAL` | Aceleran las listas de sucursales y equipos que usan las pantallas. |
| `emp/V2__mejoras_sprint1.sql` | EMP | Columna `VERSION` en `EMPLEADO` | Concurrencia optimista al editar fichas. |
| | EMP | `CHECK` del formato del RUT y del correo | Rechaza en la base los datos mal formados (casos CP-21 y CP-22). |
| | EMP | `CHECK` que impide desactivar con una fecha anterior al ingreso a la rotación | Coherencia de fechas para el cálculo de la cuota esperada `e = T × c / Σc`. |
| | EMP | Índices `IX_EMP_EMPRESA_ESTADO`, `IX_EMP_BUSQUEDA` (por `UPPER(APELLIDOS)`) e `IX_OUTEVE_PENDIENTES` | Aceleran la búsqueda de empleados (P04) y el publicador del outbox. |
| `org/V4` y `emp/V3` | ORG y EMP | Datos semilla del ambiente QA | Empresa ficticia con 3 sucursales, 5 equipos y 18 fichas para probar y para la revisión docente. Los RUT son inventados, con dígito verificador válido. |

## Decisiones que se mantienen

- **Sin llaves foráneas entre esquemas.** `EMP.EMPLEADO.SUCURSAL_ID` es una referencia lógica: `ms-empleados` le pregunta a `ms-organizacion` si la sucursal y el equipo existen y están activos antes de guardar.
- **Usuarios de ejecución con mínimo privilegio.** Los servicios se conectan como `ORG_APP` y `EMP_APP`, que solo tienen SELECT, INSERT, UPDATE y DELETE sobre su esquema. Las migraciones las aplica el usuario dueño del esquema.
- **Identidad.** La ficha, el rol, la empresa y la sucursal de cada persona se guardan en Oracle. `EMPLEADO.USUARIO_OID` guarda el identificador de su cuenta en Entra, que se crea por invitación automática al registrarla.
