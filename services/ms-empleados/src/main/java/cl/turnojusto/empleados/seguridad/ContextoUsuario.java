package cl.turnojusto.empleados.seguridad;

/**
 * Datos del usuario autenticado que usan las reglas de negocio y de aislamiento.
 *
 * @param oid         Object ID del usuario en Entra External ID (claim {@code oid})
 * @param empleadoId  ficha del usuario en el esquema EMP
 * @param empresaId   empresa (tenant) a la que pertenece; filtra todas las consultas
 * @param sucursalId  sucursal actual del usuario
 * @param equipoId    equipo actual, o {@code null}
 * @param rol         rol de la aplicación
 * @param nombre      nombre para mostrar
 * @param correo      correo del usuario
 */
public record ContextoUsuario(String oid, Long empleadoId, Long empresaId, Long sucursalId, Long equipoId,
                              Rol rol, String nombre, String correo) {

    public boolean esAdministrador() {
        return rol == Rol.ADMINISTRADOR;
    }

    public boolean esCoordinador() {
        return rol == Rol.COORDINADOR;
    }
}
