package cl.turnojusto.empleados.empleado;

import org.springframework.data.jpa.domain.Specification;

/** Filtros combinables para la búsqueda de empleados (pantalla P04 · Búsqueda). */
final class EmpleadoSpecs {

    private EmpleadoSpecs() {
    }

    static Specification<Empleado> deEmpresa(Long empresaId) {
        return (r, q, cb) -> cb.equal(r.get("empresaId"), empresaId);
    }

    static Specification<Empleado> deSucursal(Long sucursalId) {
        return sucursalId == null ? null : (r, q, cb) -> cb.equal(r.get("sucursalId"), sucursalId);
    }

    static Specification<Empleado> deEquipo(Long equipoId) {
        return equipoId == null ? null : (r, q, cb) -> cb.equal(r.get("equipoId"), equipoId);
    }

    static Specification<Empleado> conEstado(EstadoEmpleado estado) {
        return estado == null ? null : (r, q, cb) -> cb.equal(r.get("estado"), estado);
    }

    /** Busca el texto en nombres, apellidos, RUT o correo, sin distinguir mayúsculas. */
    static Specification<Empleado> texto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String patron = "%" + texto.trim().toUpperCase().replace(".", "") + "%";
        return (r, q, cb) -> cb.or(
                cb.like(cb.upper(r.get("nombres")), patron),
                cb.like(cb.upper(r.get("apellidos")), patron),
                cb.like(cb.upper(r.get("rut")), patron),
                cb.like(cb.upper(r.get("correo")), patron));
    }
}
