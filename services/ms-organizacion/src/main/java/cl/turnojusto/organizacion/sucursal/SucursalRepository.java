package cl.turnojusto.organizacion.sucursal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a ORG.SUCURSAL; siempre filtrado por empresa. */
public interface SucursalRepository extends JpaRepository<Sucursal, Long> {

    List<Sucursal> findByEmpresaIdOrderByNombreAsc(Long empresaId);

    Optional<Sucursal> findByIdAndEmpresaId(Long id, Long empresaId);

    boolean existsByEmpresaIdAndCodigoIgnoreCase(Long empresaId, String codigo);

    boolean existsByEmpresaIdAndCodigoIgnoreCaseAndIdNot(Long empresaId, String codigo, Long id);

    boolean existsByEmpresaIdAndNombreIgnoreCase(Long empresaId, String nombre);

    boolean existsByEmpresaIdAndNombreIgnoreCaseAndIdNot(Long empresaId, String nombre, Long id);
}
