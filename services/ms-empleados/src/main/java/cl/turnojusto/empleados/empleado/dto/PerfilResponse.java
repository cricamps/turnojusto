package cl.turnojusto.empleados.empleado.dto;

import cl.turnojusto.empleados.seguridad.ContextoUsuario;
import cl.turnojusto.empleados.seguridad.Rol;

/** Contexto del usuario autenticado; lo usan el frontend y los demás microservicios. */
public record PerfilResponse(Long empleadoId, Long empresaId, Long sucursalId, Long equipoId, Rol rol,
                             String nombre, String correo) {

    public static PerfilResponse de(ContextoUsuario c) {
        return new PerfilResponse(c.empleadoId(), c.empresaId(), c.sucursalId(), c.equipoId(), c.rol(), c.nombre(),
                c.correo());
    }
}
