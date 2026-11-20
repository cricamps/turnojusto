package cl.turnojusto.empleados.empleado;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso al historial de traslados (EMP.TRASLADO). */
public interface TrasladoRepository extends JpaRepository<Traslado, Long> {

    List<Traslado> findByEmpleadoIdOrderByFechaEfectivaDesc(Long empleadoId);
}
