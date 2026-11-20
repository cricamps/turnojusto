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
