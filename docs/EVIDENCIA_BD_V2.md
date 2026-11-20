# Evidencia · Script de base de datos v2 aplicado en Oracle

- **Base de datos:** TurnoJusto (`turnojustogrupo02`), Oracle Autonomous Database 19c, Siempre gratis, región Chile Central (Santiago).
- **Fecha:** 19-11-2026.
- **Qué se ejecutó:** `org/V3__mejoras_sprint1.sql`, `org/V4__datos_semilla_qa.sql`, `emp/V2__mejoras_sprint1.sql` y `emp/V3__datos_semilla_qa.sql`, en ese orden, como ADMIN desde Database Actions con `ALTER SESSION SET CURRENT_SCHEMA`.
- **Resultado:** 68 sentencias ejecutadas, 0 errores.

## Datos cargados

| Tabla | Filas |
|---|---|
| ORG.EMPRESA | 1 |
| ORG.SUCURSAL | 3 |
| ORG.EQUIPO | 5 |
| ORG.COORDINADOR_EQUIPO | 5 |
| EMP.EMPLEADO | 18 (17 activos, 1 inactivo) |
| EMP.TRASLADO | 1 |

## Pruebas de las restricciones nuevas

Cada caso se ejecutó con `ROLLBACK` al final, así que la base quedó sin datos de prueba.

| Caso | Esperado | Obtenido |
|---|---|---|
| Empleado con RUT con puntos (`12.345.678-5`) | Rechazo | ORA-02290 `EMP.CK_EMPLEADO_4` ✔ |
| Empleado con correo inválido | Rechazo | ORA-02290 `EMP.CK_EMPLEADO_5` ✔ |
| RUT repetido en la empresa | Rechazo | ORA-00001 `EMP.UQ_EMPLEADO_1` ✔ |
| Desactivación con fecha anterior al ingreso | Rechazo | ORA-02290 `EMP.CK_EMPLEADO_6` ✔ |
| Segundo coordinador vigente en el mismo equipo | Rechazo | ORA-00001 `ORG.UX_COOEQU_VIGENTE` ✔ |
| Equipo de una empresa que no existe | Rechazo | ORA-02291 `ORG.FK_EQU_EMP_EMPRES` ✔ |
| RUT de empresa con puntos | Rechazo | ORA-02290 `ORG.CK_EMPRESA_2` ✔ |
| Empleado válido (control) | Aceptado | OK, con identificador generado después de 3017 ✔ |

## Conexión de los microservicios

- Autenticación **TLS** (puerto 1521) sin billetera. La lista de control de acceso está activada; los contenedores de Fargate no tienen una IP de salida fija, por eso la regla permite cualquier origen y el acceso queda protegido con TLS y contraseña.
- `ms-organizacion` se conecta como `ORG_APP` y `ms-empleados` como `EMP_APP`. Ambos usuarios solo tienen permisos DML sobre su propio esquema.
- La URL JDBC está en la variable `TJ_DB_URL` del repositorio. Las contraseñas viven en GitHub Secrets y en AWS Secrets Manager, nunca en el código.

## Doble aprobación de reemplazos · `tur/V2` (retroalimentación de la semana 4)

- **Fecha:** 20-11-2026.
- **Qué se ejecutó:** `tur/V2__doble_aprobacion_reemplazos.sql`, como ADMIN desde Database Actions con `ALTER SESSION SET CURRENT_SCHEMA = TUR`.
- **Resultado:** 34 sentencias, 0 errores. `TUR.REEMPLAZO` queda con 8 restricciones `CK_REEMPLAZO_*` habilitadas, la columna virtual `TIPO` y los índices `IX_REE_PEND_DESTINO` e `IX_REE_PEND_ORIGEN`.
- **Pruebas:** `db/pruebas/tur_V2_pruebas_doble_aprobacion.sql`. Se crearon un turno de prueba (Valentina Rojas, Cajas Centro, sábado 05-12-2026) y 14 solicitudes, y al final se hizo `ROLLBACK`. Después se verificó que `REEMPLAZO`, `ASIGNACION_TURNO`, `CALENDARIO` y `CONFIG_TURNO` quedaran con 0 filas.

| Caso | Esperado | Obtenido |
|---|---|---|
| P01 Interno recién solicitado | Aceptado | OK, `TIPO = INTERNO` ✔ |
| P02 Interno aprobado con una sola decisión (destino) | Aceptado | OK ✔ |
| P03 Interno con decisión de origen | Rechazo | ORA-02290 `TUR.CK_REEMPLAZO_6` ✔ |
| P04 Entre sucursales `APROBADO` con una sola aprobación | Rechazo | ORA-02290 `TUR.CK_REEMPLAZO_8` ✔ |
| P05 Entre sucursales con destino aprobado y origen pendiente (`APROBACION_PARCIAL`) | Aceptado | OK, `TIPO = ENTRE_SUCURSALES` ✔ |
| P06 Entre sucursales aprobado por ambos coordinadores | Aceptado | OK ✔ |
| P07 Origen rechaza sin motivo | Rechazo | ORA-02290 `TUR.CK_REEMPLAZO_5` ✔ |
| P08 Destino aprobó y origen rechaza con motivo → `RECHAZADO` | Aceptado | OK ✔ |
| P09 Decisión de origen sin fecha | Rechazo | ORA-02290 `TUR.CK_REEMPLAZO_4` ✔ |
| P10 La misma persona decide en origen y en destino | Rechazo | ORA-02290 `TUR.CK_REEMPLAZO_7` ✔ |
| P11 El reemplazante aprueba su propio reemplazo | Rechazo | ORA-02290 `TUR.CK_REEMPLAZO_7` ✔ |
| P12 Interno en `APROBACION_PARCIAL` | Rechazo | ORA-02290 `TUR.CK_REEMPLAZO_8` ✔ |
| P13 `APROBADO` sin ninguna decisión | Rechazo | ORA-02290 `TUR.CK_REEMPLAZO_8` ✔ |
| P14 `VENCIDO` con la aprobación de origen pendiente | Aceptado | OK ✔ |
