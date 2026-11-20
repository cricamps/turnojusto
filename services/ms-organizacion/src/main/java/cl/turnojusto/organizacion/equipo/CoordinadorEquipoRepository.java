package cl.turnojusto.organizacion.equipo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a ORG.COORDINADOR_EQUIPO. */
public interface CoordinadorEquipoRepository extends JpaRepository<CoordinadorEquipo, Long> {

    Optional<CoordinadorEquipo> findByEquipoIdAndVigenteHastaIsNull(Long equipoId);

    List<CoordinadorEquipo> findByEmpresaIdAndVigenteHastaIsNull(Long empresaId);

    List<CoordinadorEquipo> findByEquipoIdOrderByVigenteDesdeDesc(Long equipoId);
}
