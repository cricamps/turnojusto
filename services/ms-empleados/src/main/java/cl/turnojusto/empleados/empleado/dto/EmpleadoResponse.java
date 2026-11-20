package cl.turnojusto.empleados.empleado.dto;

import cl.turnojusto.empleados.empleado.Empleado;
import cl.turnojusto.empleados.empleado.EstadoEmpleado;
import cl.turnojusto.empleados.seguridad.Rol;
import java.time.LocalDate;

/** Ficha del empleado tal como la consume el frontend. */
public record EmpleadoResponse(Long id, Long empresaId, Long sucursalId, Long equipoId, String rut, String nombres,
                               String apellidos, String correo, String telefono, String cargo, Rol rol,
                               LocalDate fechaIngresoRotacion, EstadoEmpleado estado, LocalDate fechaDesactivacion,
                               String motivoDesactivacion, boolean cuentaVinculada, Long version) {

    public static EmpleadoResponse de(Empleado e) {
        return new EmpleadoResponse(e.getId(), e.getEmpresaId(), e.getSucursalId(), e.getEquipoId(), e.getRut(),
                e.getNombres(), e.getApellidos(), e.getCorreo(), e.getTelefono(), e.getCargo(), e.getRol(),
                e.getFechaIngresoRotacion(), e.getEstado(), e.getFechaDesactivacion(), e.getMotivoDesactivacion(),
                e.getUsuarioOid() != null, e.getVersion());
    }
}
