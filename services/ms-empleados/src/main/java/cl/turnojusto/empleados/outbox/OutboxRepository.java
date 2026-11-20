package cl.turnojusto.empleados.outbox;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a EMP.OUTBOX_EVENTO. */
public interface OutboxRepository extends JpaRepository<OutboxEvento, String> {

    List<OutboxEvento> findTop20ByEstadoOrderByCreadoEnAsc(String estado);
}
