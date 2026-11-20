-- =====================================================================
-- TurnoJusto · Modelo de datos físico (Oracle Autonomous Database 19c/23ai)
-- Taller Aplicado de Software (TSY2201) · Semana 4 · Cynthia Torres / Cristobal Camps
-- Esquema REP · V2 · Vista del consolidado de empresa
-- =====================================================================

-- Consolidado mensual por empresa, calculado desde los resúmenes de sucursal.
CREATE OR REPLACE VIEW V_CONSOLIDADO_EMPRESA AS
SELECT EMPRESA_ID,
       ANIO,
       MES,
       COUNT(*)                                   AS SUCURSALES,
       SUM(TOTAL_TURNOS_PERSONA)                  AS TOTAL_TURNOS_PERSONA,
       SUM(EMPLEADOS_ACTIVOS)                     AS EMPLEADOS_ACTIVOS,
       ROUND(SUM(TOTAL_TURNOS_PERSONA) / NULLIF(SUM(EMPLEADOS_ACTIVOS), 0), 2) AS PROMEDIO_TURNOS_PERSONA,
       MAX(DESVIACION_MAXIMA)                     AS DESVIACION_MAXIMA,
       SUM(EXCEPCIONES)                           AS EXCEPCIONES,
       ROUND(100 * SUM(EXCEPCIONES) / NULLIF(SUM(TOTAL_TURNOS_PERSONA), 0), 2) AS PCT_EXCEPCIONES,
       SUM(REEMPLAZOS_INTERSUCURSAL)              AS REEMPLAZOS_INTERSUCURSAL
  FROM RESUMEN_SUCURSAL
 GROUP BY EMPRESA_ID, ANIO, MES;

GRANT SELECT ON V_CONSOLIDADO_EMPRESA TO REP_APP;
