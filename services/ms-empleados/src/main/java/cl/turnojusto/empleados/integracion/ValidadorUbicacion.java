package cl.turnojusto.empleados.integracion;

/**
 * Puerto de salida: confirma con ms-organizacion que la sucursal y el equipo existen, están
 * activos y pertenecen a la empresa del usuario (no hay llaves foráneas entre esquemas).
 */
public interface ValidadorUbicacion {

    void validar(Long sucursalId, Long equipoId);
}
