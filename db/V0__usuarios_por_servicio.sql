-- =====================================================================
-- TurnoJusto · Modelo de datos físico (Oracle Autonomous Database 19c/23ai)
-- Taller Aplicado de Software (TSY2201) · Semana 4 · Cynthia Torres / Cristobal Camps
-- V0 · Usuarios por microservicio (ejecutar como ADMIN)
-- =====================================================================

-- Cada microservicio tiene dos usuarios:
--   <ESQ>      dueño del esquema; lo usa solo Flyway en el pipeline para crear y migrar tablas.
--   <ESQ>_APP  usuario de ejecución del servicio; solo DML sobre su propio esquema (mínimo privilegio).
-- Las contraseñas se generan en Terraform y se guardan en AWS Secrets Manager; aquí van como variables.
-- Ningún usuario recibe permisos sobre el esquema de otro servicio.

-- ORG · ms-organizacion
CREATE USER ORG IDENTIFIED BY "&pwd_org" QUOTA 2G ON DATA;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO ORG;
CREATE USER ORG_APP IDENTIFIED BY "&pwd_org_app";
GRANT CREATE SESSION TO ORG_APP;
ALTER USER ORG_APP DEFAULT ROLE ALL;

-- EMP · ms-empleados
CREATE USER EMP IDENTIFIED BY "&pwd_emp" QUOTA 2G ON DATA;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO EMP;
CREATE USER EMP_APP IDENTIFIED BY "&pwd_emp_app";
GRANT CREATE SESSION TO EMP_APP;
ALTER USER EMP_APP DEFAULT ROLE ALL;

-- TUR · ms-turnos
CREATE USER TUR IDENTIFIED BY "&pwd_tur" QUOTA 2G ON DATA;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO TUR;
CREATE USER TUR_APP IDENTIFIED BY "&pwd_tur_app";
GRANT CREATE SESSION TO TUR_APP;
ALTER USER TUR_APP DEFAULT ROLE ALL;

-- ASG · ms-asignacion
CREATE USER ASG IDENTIFIED BY "&pwd_asg" QUOTA 2G ON DATA;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO ASG;
CREATE USER ASG_APP IDENTIFIED BY "&pwd_asg_app";
GRANT CREATE SESSION TO ASG_APP;
ALTER USER ASG_APP DEFAULT ROLE ALL;

-- REP · ms-reportes
CREATE USER REP IDENTIFIED BY "&pwd_rep" QUOTA 2G ON DATA;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO REP;
CREATE USER REP_APP IDENTIFIED BY "&pwd_rep_app";
GRANT CREATE SESSION TO REP_APP;
ALTER USER REP_APP DEFAULT ROLE ALL;
