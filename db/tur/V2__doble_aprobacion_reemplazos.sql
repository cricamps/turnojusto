-- =====================================================================
-- TurnoJusto · Esquema TUR · ms-turnos
-- V2 · Doble aprobación de reemplazos entre sucursales (retroalimentación semana 4)
-- Se ejecuta conectado como el usuario dueño TUR (Flyway).
-- =====================================================================
-- Problema: en V1 la solicitud tenía una sola resolución (RESUELTO_POR y
-- RESUELTO_EN). Un reemplazo entre sucursales necesita dos decisiones
-- independientes. Por ejemplo, si una trabajadora de Maipú cubre un turno en
-- Centro:
--   * ORIGEN : el coordinador de Maipú (sucursal del reemplazante) autoriza su SALIDA.
--   * DESTINO: el coordinador de Centro (sucursal del turno) acepta su INCORPORACIÓN.
-- Un reemplazo interno (misma sucursal) solo requiere la decisión de DESTINO.
--
-- Estados:
--   SOLICITADO          ninguna decisión registrada.
--   APROBACION_PARCIAL  entre sucursales, con una aprobación registrada y la otra pendiente.
--                       El turno sigue asignado al solicitante.
--   APROBADO            todas las aprobaciones requeridas están registradas. Solo en este
--                       estado ms-turnos cambia ASIGNACION_TURNO (EMPLEADO_EFECTIVO_ID y
--                       ESTADO = 'REEMPLAZADO'), en la misma transacción, y publica
--                       ReemplazoAprobado.
--   RECHAZADO           un coordinador rechazó (con comentario obligatorio). El estado es
--                       final: la decisión pendiente ya no se pide, el turno queda con el
--                       solicitante y se publica ReemplazoRechazado para avisarle.
--   CANCELADO           el solicitante retiró la solicitud antes de completarla.
--   VENCIDO             el turno empezó sin todas las aprobaciones; lo marca un proceso
--                       programado y el turno queda con el solicitante.
-- Las reglas se aseguran con CHECK en la misma fila, así la base rechaza cualquier
-- combinación incoherente aunque falle la lógica del servicio.
-- =====================================================================

-- 1. Se retiran la resolución única de V1 y sus restricciones
ALTER TABLE REEMPLAZO DROP CONSTRAINT CK_REEMPLAZO_1;
ALTER TABLE REEMPLAZO DROP CONSTRAINT CK_REEMPLAZO_3;
ALTER TABLE REEMPLAZO DROP CONSTRAINT CK_REEMPLAZO_4;
ALTER TABLE REEMPLAZO DROP (RESUELTO_POR, RESUELTO_EN, COMENTARIO_RESOLUCION);
ALTER TABLE REEMPLAZO MODIFY (ESTADO VARCHAR2(20));

-- 2. Tipo de reemplazo (columna virtual: no se puede contradecir con las sucursales)
ALTER TABLE REEMPLAZO ADD (
    TIPO VARCHAR2(16) GENERATED ALWAYS AS (
        CASE WHEN SUCURSAL_ID = SUCURSAL_REEMPLAZANTE_ID THEN 'INTERNO' ELSE 'ENTRE_SUCURSALES' END
    ) VIRTUAL
);

-- 3. Decisión de DESTINO (sucursal del turno): acepta la incorporación. Se exige siempre.
ALTER TABLE REEMPLAZO ADD (
    DECISION_DESTINO           VARCHAR2(10),
    DESTINO_COORDINADOR_ID     NUMBER(19),
    DESTINO_DECIDIDO_POR       VARCHAR2(36),
    DESTINO_DECIDIDO_EN        TIMESTAMP WITH TIME ZONE,
    DESTINO_COMENTARIO         VARCHAR2(300)
);

-- 4. Decisión de ORIGEN (sucursal del reemplazante): autoriza la salida. Solo entre sucursales.
ALTER TABLE REEMPLAZO ADD (
    DECISION_ORIGEN            VARCHAR2(10),
    ORIGEN_COORDINADOR_ID      NUMBER(19),
    ORIGEN_DECIDIDO_POR        VARCHAR2(36),
    ORIGEN_DECIDIDO_EN         TIMESTAMP WITH TIME ZONE,
    ORIGEN_COMENTARIO          VARCHAR2(300)
);

-- 5. Concurrencia: los dos coordinadores pueden decidir al mismo tiempo
ALTER TABLE REEMPLAZO ADD (VERSION NUMBER(10) DEFAULT 0 NOT NULL);

-- 6. Reglas
ALTER TABLE REEMPLAZO ADD CONSTRAINT CK_REEMPLAZO_1 CHECK (
    ESTADO IN ('SOLICITADO','APROBACION_PARCIAL','APROBADO','RECHAZADO','CANCELADO','VENCIDO'));

-- Cada decisión se registra completa: valor, coordinador (ficha y cuenta) y fecha
ALTER TABLE REEMPLAZO ADD CONSTRAINT CK_REEMPLAZO_3 CHECK (
    (DECISION_DESTINO IS NULL AND DESTINO_COORDINADOR_ID IS NULL AND DESTINO_DECIDIDO_POR IS NULL AND DESTINO_DECIDIDO_EN IS NULL)
 OR (DECISION_DESTINO IN ('APROBADO','RECHAZADO') AND DESTINO_COORDINADOR_ID IS NOT NULL
     AND DESTINO_DECIDIDO_POR IS NOT NULL AND DESTINO_DECIDIDO_EN IS NOT NULL));
ALTER TABLE REEMPLAZO ADD CONSTRAINT CK_REEMPLAZO_4 CHECK (
    (DECISION_ORIGEN IS NULL AND ORIGEN_COORDINADOR_ID IS NULL AND ORIGEN_DECIDIDO_POR IS NULL AND ORIGEN_DECIDIDO_EN IS NULL)
 OR (DECISION_ORIGEN IN ('APROBADO','RECHAZADO') AND ORIGEN_COORDINADOR_ID IS NOT NULL
     AND ORIGEN_DECIDIDO_POR IS NOT NULL AND ORIGEN_DECIDIDO_EN IS NOT NULL));

-- Un rechazo siempre explica el motivo
ALTER TABLE REEMPLAZO ADD CONSTRAINT CK_REEMPLAZO_5 CHECK (
    (DECISION_DESTINO <> 'RECHAZADO' OR DESTINO_COMENTARIO IS NOT NULL)
AND (DECISION_ORIGEN <> 'RECHAZADO' OR ORIGEN_COMENTARIO IS NOT NULL));

-- Un reemplazo interno no tiene decisión de origen
ALTER TABLE REEMPLAZO ADD CONSTRAINT CK_REEMPLAZO_6 CHECK (
    TIPO = 'ENTRE_SUCURSALES' OR DECISION_ORIGEN IS NULL);

-- Las dos decisiones las toman personas distintas y ninguna es parte interesada
ALTER TABLE REEMPLAZO ADD CONSTRAINT CK_REEMPLAZO_7 CHECK (
    (ORIGEN_COORDINADOR_ID IS NULL OR DESTINO_COORDINADOR_ID IS NULL OR ORIGEN_COORDINADOR_ID <> DESTINO_COORDINADOR_ID)
AND (DESTINO_COORDINADOR_ID IS NULL OR DESTINO_COORDINADOR_ID NOT IN (SOLICITANTE_ID, REEMPLAZANTE_ID))
AND (ORIGEN_COORDINADOR_ID IS NULL OR ORIGEN_COORDINADOR_ID NOT IN (SOLICITANTE_ID, REEMPLAZANTE_ID)));

-- El estado coincide siempre con las decisiones registradas.
-- Se usa NVL para que una decisión vacía nunca deje la condición en NULL
-- (Oracle acepta un CHECK cuyo resultado es NULL).
ALTER TABLE REEMPLAZO ADD CONSTRAINT CK_REEMPLAZO_8 CHECK (
    (ESTADO = 'SOLICITADO'
        AND NVL(DECISION_DESTINO, '-') = '-' AND NVL(DECISION_ORIGEN, '-') = '-')
 OR (ESTADO = 'APROBACION_PARCIAL' AND TIPO = 'ENTRE_SUCURSALES'
        AND ((NVL(DECISION_DESTINO, '-') = 'APROBADO' AND NVL(DECISION_ORIGEN, '-') = '-')
          OR (NVL(DECISION_ORIGEN, '-') = 'APROBADO' AND NVL(DECISION_DESTINO, '-') = '-')))
 OR (ESTADO = 'APROBADO'
        AND NVL(DECISION_DESTINO, '-') = 'APROBADO'
        AND (TIPO = 'INTERNO' OR NVL(DECISION_ORIGEN, '-') = 'APROBADO'))
 OR (ESTADO = 'RECHAZADO'
        AND (NVL(DECISION_DESTINO, '-') = 'RECHAZADO' OR NVL(DECISION_ORIGEN, '-') = 'RECHAZADO'))
 OR (ESTADO IN ('CANCELADO','VENCIDO')
        AND NVL(DECISION_DESTINO, '-') <> 'RECHAZADO' AND NVL(DECISION_ORIGEN, '-') <> 'RECHAZADO'
        AND NOT (NVL(DECISION_DESTINO, '-') = 'APROBADO'
                 AND (TIPO = 'INTERNO' OR NVL(DECISION_ORIGEN, '-') = 'APROBADO'))));

-- 7. Índices para las bandejas de cada coordinador
CREATE INDEX IX_REE_PEND_DESTINO ON REEMPLAZO (EMPRESA_ID, SUCURSAL_ID, ESTADO);
CREATE INDEX IX_REE_PEND_ORIGEN ON REEMPLAZO (EMPRESA_ID, SUCURSAL_REEMPLAZANTE_ID, ESTADO);

-- 8. Documentación
COMMENT ON TABLE REEMPLAZO IS 'Solicitud de reemplazo de un turno; entre sucursales requiere la aprobación de ambos coordinadores';
COMMENT ON COLUMN REEMPLAZO.ESTADO IS 'SOLICITADO, APROBACION_PARCIAL, APROBADO, RECHAZADO, CANCELADO o VENCIDO';
COMMENT ON COLUMN REEMPLAZO.TIPO IS 'INTERNO (misma sucursal, una aprobación) o ENTRE_SUCURSALES (dos aprobaciones)';
COMMENT ON COLUMN REEMPLAZO.DECISION_DESTINO IS 'Decisión del coordinador de la sucursal del turno: acepta la incorporación';
COMMENT ON COLUMN REEMPLAZO.DESTINO_COORDINADOR_ID IS 'Ficha (EMP.EMPLEADO) del coordinador que decidió en destino';
COMMENT ON COLUMN REEMPLAZO.DESTINO_DECIDIDO_POR IS 'OID de Entra de quien decidió en destino';
COMMENT ON COLUMN REEMPLAZO.DESTINO_DECIDIDO_EN IS 'Momento de la decisión de destino';
COMMENT ON COLUMN REEMPLAZO.DESTINO_COMENTARIO IS 'Motivo; obligatorio si se rechaza';
COMMENT ON COLUMN REEMPLAZO.DECISION_ORIGEN IS 'Decisión del coordinador de la sucursal del reemplazante: autoriza la salida';
COMMENT ON COLUMN REEMPLAZO.ORIGEN_COORDINADOR_ID IS 'Ficha (EMP.EMPLEADO) del coordinador que decidió en origen';
COMMENT ON COLUMN REEMPLAZO.ORIGEN_DECIDIDO_POR IS 'OID de Entra de quien decidió en origen';
COMMENT ON COLUMN REEMPLAZO.ORIGEN_DECIDIDO_EN IS 'Momento de la decisión de origen';
COMMENT ON COLUMN REEMPLAZO.ORIGEN_COMENTARIO IS 'Motivo; obligatorio si se rechaza';
COMMENT ON COLUMN REEMPLAZO.VERSION IS 'Versión del registro para bloqueo optimista (dos coordinadores decidiendo a la vez)';
COMMENT ON COLUMN OUTBOX_EVENTO.TIPO_EVENTO IS 'TurnoPublicado, ReemplazoSolicitado, ReemplazoAprobacionParcial, ReemplazoAprobado, ReemplazoRechazado, ReemplazoVencido';
