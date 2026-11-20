package cl.turnojusto.organizacion.integracion;

import java.util.Optional;

/** Puerto de salida hacia ms-empleados (no hay llaves foráneas entre esquemas). */
public interface ConsultaEmpleados {

    /** Ficha resumida de un empleado de la empresa del usuario, si existe. */
    Optional<EmpleadoResumen> buscar(Long empleadoId);

    /** Datos del empleado que importan para asignarlo como coordinador. */
    record EmpleadoResumen(Long id, Long sucursalId, String rol, String estado) {
    }
}
