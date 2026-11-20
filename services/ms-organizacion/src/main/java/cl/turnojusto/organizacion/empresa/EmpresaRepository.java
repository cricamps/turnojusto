package cl.turnojusto.organizacion.empresa;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a ORG.EMPRESA. */
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
}
