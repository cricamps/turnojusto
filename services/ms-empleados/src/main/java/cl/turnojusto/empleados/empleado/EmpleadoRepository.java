package cl.turnojusto.empleados.empleado;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Acceso a EMP.EMPLEADO. Todas las consultas de negocio filtran por empresa. */
public interface EmpleadoRepository extends JpaRepository<Empleado, Long>, JpaSpecificationExecutor<Empleado> {

    Optional<Empleado> findByUsuarioOid(String usuarioOid);

    List<Empleado> findByCorreoIgnoreCaseAndUsuarioOidIsNull(String correo);

    Optional<Empleado> findByIdAndEmpresaId(Long id, Long empresaId);

    boolean existsByEmpresaIdAndRut(Long empresaId, String rut);

    boolean existsByEmpresaIdAndRutAndIdNot(Long empresaId, String rut, Long id);

    boolean existsByEmpresaIdAndCorreoIgnoreCase(Long empresaId, String correo);

    boolean existsByEmpresaIdAndCorreoIgnoreCaseAndIdNot(Long empresaId, String correo, Long id);

    long countByEmpresaIdAndEstado(Long empresaId, EstadoEmpleado estado);
}
