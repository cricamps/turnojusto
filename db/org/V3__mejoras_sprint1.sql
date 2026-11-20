-- =====================================================================
-- TurnoJusto · Esquema ORG · ms-organizacion
-- V3 · Mejoras al modelo detectadas al programar el Sprint 1 (Semana 5)
-- Se ejecuta conectado como el usuario dueño ORG (Flyway).
-- =====================================================================
-- Cambios respecto de la versión entregada en la Semana 4:
--   1. Llaves foráneas que faltaban hacia EMPRESA en EQUIPO y COORDINADOR_EQUIPO
--      (antes solo se validaba en la API; ahora también lo asegura la base).
--   2. Columna VERSION para control de concurrencia optimista (@Version en JPA):
--      si dos administradores editan la misma sucursal, el segundo recibe 409.
--   3. Formato normalizado del RUT (sin puntos, con guion y DV en mayúscula).
--   4. Un solo coordinador vigente por equipo (índice único por función).
--   5. Índices para las búsquedas que usan las pantallas (empresa + estado).
-- =====================================================================

-- 1. Llaves foráneas hacia EMPRESA
ALTER TABLE EQUIPO ADD CONSTRAINT FK_EQU_EMP_EMPRES
    FOREIGN KEY (EMPRESA_ID) REFERENCES EMPRESA (EMPRESA_ID);
ALTER TABLE COORDINADOR_EQUIPO ADD CONSTRAINT FK_COOEQU_EMP_EMPRES
    FOREIGN KEY (EMPRESA_ID) REFERENCES EMPRESA (EMPRESA_ID);

-- 2. Control de concurrencia optimista
ALTER TABLE EMPRESA  ADD (VERSION NUMBER(10) DEFAULT 0 NOT NULL);
ALTER TABLE SUCURSAL ADD (VERSION NUMBER(10) DEFAULT 0 NOT NULL);
ALTER TABLE EQUIPO   ADD (VERSION NUMBER(10) DEFAULT 0 NOT NULL);
COMMENT ON COLUMN EMPRESA.VERSION  IS 'Versión del registro para bloqueo optimista';
COMMENT ON COLUMN SUCURSAL.VERSION IS 'Versión del registro para bloqueo optimista';
COMMENT ON COLUMN EQUIPO.VERSION   IS 'Versión del registro para bloqueo optimista';

-- 3. RUT normalizado: 76123456-0 (7 u 8 dígitos, guion y dígito verificador)
ALTER TABLE EMPRESA ADD CONSTRAINT CK_EMPRESA_2
    CHECK (REGEXP_LIKE(RUT, '^[1-9][0-9]{6,7}-[0-9K]$'));
COMMENT ON COLUMN EMPRESA.RUT IS 'RUT normalizado sin puntos, con guion y DV (76123456-0); el DV lo valida la API';

-- Correo de contacto con formato válido
ALTER TABLE EMPRESA ADD CONSTRAINT CK_EMPRESA_3
    CHECK (REGEXP_LIKE(CORREO_CONTACTO, '^[^@ ]+@[^@ ]+\.[^@ ]+$'));

-- 4. Un solo coordinador vigente (VIGENTE_HASTA nulo) por equipo
CREATE UNIQUE INDEX UX_COOEQU_VIGENTE ON COORDINADOR_EQUIPO
    (CASE WHEN VIGENTE_HASTA IS NULL THEN EQUIPO_ID END);

-- 5. Índices para las pantallas de búsqueda
CREATE INDEX IX_SUC_EMPRESA_ESTADO ON SUCURSAL (EMPRESA_ID, ESTADO);
CREATE INDEX IX_EQU_EMPRESA_SUCURSAL ON EQUIPO (EMPRESA_ID, SUCURSAL_ID, ESTADO);
