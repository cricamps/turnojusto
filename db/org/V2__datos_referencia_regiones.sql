-- =====================================================================
-- TurnoJusto · Modelo de datos físico (Oracle Autonomous Database 19c/23ai)
-- Taller Aplicado de Software (TSY2201) · Semana 4 · Cynthia Torres / Cristobal Camps
-- Esquema ORG · V2 · Datos de referencia
-- =====================================================================

-- Catálogo de regiones (los feriados se cargan desde ms-organizacion cada año).
INSERT INTO REGION (REGION_CODIGO, NOMBRE, ORDEN) VALUES ('CL-AP', 'Arica y Parinacota', 1);
INSERT INTO REGION (REGION_CODIGO, NOMBRE, ORDEN) VALUES ('CL-TA', 'Tarapacá', 2);
INSERT INTO REGION (REGION_CODIGO, NOMBRE, ORDEN) VALUES ('CL-AN', 'Antofagasta', 3);
INSERT INTO REGION (REGION_CODIGO, NOMBRE, ORDEN) VALUES ('CL-AT', 'Atacama', 4);
INSERT INTO REGION (REGION_CODIGO, NOMBRE, ORDEN) VALUES ('CL-CO', 'Coquimbo', 5);
INSERT INTO REGION (REGION_CODIGO, NOMBRE, ORDEN) VALUES ('CL-VS', 'Valparaíso', 6);
INSERT INTO REGION (REGION_CODIGO, NOMBRE, ORDEN) VALUES ('CL-RM', 'Metropolitana de Santiago', 7);
INSERT INTO REGION (REGION_CODIGO, NOMBRE, ORDEN) VALUES ('CL-LI', 'Libertador General Bernardo O''Higgins', 8);
INSERT INTO REGION (REGION_CODIGO, NOMBRE, ORDEN) VALUES ('CL-ML', 'Maule', 9);
INSERT INTO REGION (REGION_CODIGO, NOMBRE, ORDEN) VALUES ('CL-NB', 'Ñuble', 10);
INSERT INTO REGION (REGION_CODIGO, NOMBRE, ORDEN) VALUES ('CL-BI', 'Biobío', 11);
INSERT INTO REGION (REGION_CODIGO, NOMBRE, ORDEN) VALUES ('CL-AR', 'La Araucanía', 12);
INSERT INTO REGION (REGION_CODIGO, NOMBRE, ORDEN) VALUES ('CL-LR', 'Los Ríos', 13);
INSERT INTO REGION (REGION_CODIGO, NOMBRE, ORDEN) VALUES ('CL-LL', 'Los Lagos', 14);
INSERT INTO REGION (REGION_CODIGO, NOMBRE, ORDEN) VALUES ('CL-AI', 'Aysén del General Carlos Ibáñez del Campo', 15);
INSERT INTO REGION (REGION_CODIGO, NOMBRE, ORDEN) VALUES ('CL-MA', 'Magallanes y de la Antártica Chilena', 16);
COMMIT;
