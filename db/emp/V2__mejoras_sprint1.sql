-- =====================================================================
-- TurnoJusto · Esquema EMP · ms-empleados
-- V2 · Mejoras al modelo detectadas al programar el Sprint 1 (Semana 5)
-- Se ejecuta conectado como el usuario dueño EMP (Flyway).
-- =====================================================================
-- Cambios respecto de la versión entregada en la Semana 4:
--   1. Columna VERSION para control de concurrencia optimista (@Version en JPA).
--   2. Formato normalizado del RUT y del correo.
--   3. La fecha de desactivación no puede ser anterior al ingreso a la rotación.
--   4. Índices para la búsqueda de empleados (empresa + estado, apellidos sin
--      distinguir mayúsculas) y para el publicador del outbox.
-- =====================================================================

-- 1. Control de concurrencia optimista
ALTER TABLE EMPLEADO ADD (VERSION NUMBER(10) DEFAULT 0 NOT NULL);
COMMENT ON COLUMN EMPLEADO.VERSION IS 'Versión del registro para bloqueo optimista';

-- 2. Formatos
ALTER TABLE EMPLEADO ADD CONSTRAINT CK_EMPLEADO_4
    CHECK (REGEXP_LIKE(RUT, '^[1-9][0-9]{6,7}-[0-9K]$'));
ALTER TABLE EMPLEADO ADD CONSTRAINT CK_EMPLEADO_5
    CHECK (REGEXP_LIKE(CORREO, '^[^@ ]+@[^@ ]+\.[^@ ]+$'));
COMMENT ON COLUMN EMPLEADO.RUT IS 'RUT normalizado sin puntos, con guion y DV (12345678-5); el DV lo valida la API';
COMMENT ON COLUMN EMPLEADO.CORREO IS 'Correo en minúsculas; al primer ingreso vincula la cuenta de Entra (USUARIO_OID)';

-- 3. Coherencia de fechas
ALTER TABLE EMPLEADO ADD CONSTRAINT CK_EMPLEADO_6
    CHECK (FECHA_DESACTIVACION IS NULL OR FECHA_DESACTIVACION >= FECHA_INGRESO_ROTACION);

-- 4. Índices
CREATE INDEX IX_EMP_EMPRESA_ESTADO ON EMPLEADO (EMPRESA_ID, ESTADO, SUCURSAL_ID);
CREATE INDEX IX_EMP_BUSQUEDA ON EMPLEADO (EMPRESA_ID, UPPER(APELLIDOS));
CREATE INDEX IX_OUTEVE_PENDIENTES ON OUTBOX_EVENTO (ESTADO, CREADO_EN);
