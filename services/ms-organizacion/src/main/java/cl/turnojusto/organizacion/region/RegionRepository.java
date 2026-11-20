package cl.turnojusto.organizacion.region;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso al catálogo de regiones. */
public interface RegionRepository extends JpaRepository<Region, String> {

    List<Region> findAllByOrderByOrdenAsc();
}
