package cl.turnojusto.empleados.seguridad;

import cl.turnojusto.empleados.empleado.Empleado;
import cl.turnojusto.empleados.empleado.EmpleadoRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Obtiene el contexto del usuario a partir del token de Entra External ID.
 *
 * <p>Busca la ficha por el {@code oid} del token. Si es el primer ingreso de la persona, la
 * busca por correo y guarda el {@code oid} en la ficha (vinculación): así el administrador
 * registra al empleado con su correo y la cuenta queda asociada cuando inicia sesión.</p>
 */
@Component
public class ResolutorContexto {

    private static final Logger LOG = LoggerFactory.getLogger(ResolutorContexto.class);

    private final EmpleadoRepository repositorio;

    public ResolutorContexto(EmpleadoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public Optional<ContextoUsuario> resolver(Jwt jwt) {
        String oid = Optional.ofNullable(jwt.getClaimAsString("oid")).orElse(jwt.getSubject());
        Optional<Empleado> empleado = repositorio.findByUsuarioOid(oid);
        if (empleado.isEmpty()) {
            empleado = vincularPorCorreo(oid, correoDelToken(jwt));
        }
        return empleado
                .filter(Empleado::estaActivo)
                .map(e -> new ContextoUsuario(oid, e.getId(), e.getEmpresaId(), e.getSucursalId(),
                        e.getEquipoId(), e.getRol(), e.getNombres() + " " + e.getApellidos(), e.getCorreo()));
    }

    private Optional<Empleado> vincularPorCorreo(String oid, String correo) {
        if (correo == null) {
            return Optional.empty();
        }
        List<Empleado> candidatos = repositorio.findByCorreoIgnoreCaseAndUsuarioOidIsNull(correo);
        if (candidatos.size() != 1) {
            // Sin ficha, o el mismo correo en dos empresas: no se vincula automáticamente.
            return Optional.empty();
        }
        Empleado e = candidatos.getFirst();
        e.vincularCuenta(oid);
        LOG.info("Cuenta de Entra vinculada a la ficha del empleado {}", e.getId());
        return Optional.of(repositorio.save(e));
    }

    /** Entra External ID puede enviar el correo en distintos claims según la configuración. */
    static String correoDelToken(Jwt jwt) {
        for (String claim : List.of("email", "preferred_username", "upn", "unique_name")) {
            String valor = jwt.getClaimAsString(claim);
            if (valor != null && valor.contains("@")) {
                return valor.toLowerCase();
            }
        }
        List<String> emails = jwt.getClaimAsStringList("emails");
        return emails == null || emails.isEmpty() ? null : emails.getFirst().toLowerCase();
    }
}
