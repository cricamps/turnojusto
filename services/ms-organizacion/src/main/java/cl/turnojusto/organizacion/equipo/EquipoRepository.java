package cl.turnojusto.organizacion.equipo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a ORG.EQUIPO. */
public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    List<Equipo> findByEmpresaIdOrderByNombreAsc(Long empresaId);

    List<Equipo> findByEmpresaIdAndSucursalIdOrderByNombreAsc(Long empresaId, Long sucursalId);

    Optional<Equipo> findByIdAndEmpresaId(Long id, Long empresaId);

    boolean existsBySucursalIdAndNombreIgnoreCase(Long sucursalId, String nombre);

    boolean existsBySucursalIdAndNombreIgnoreCaseAndIdNot(Long sucursalId, String nombre, Long id);

    long countBySucursalIdAndEstado(Long sucursalId, String estado);
}
