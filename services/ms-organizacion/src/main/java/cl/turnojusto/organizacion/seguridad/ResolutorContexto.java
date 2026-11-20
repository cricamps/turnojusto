package cl.turnojusto.organizacion.seguridad;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Obtiene empresa, sucursal y rol del usuario preguntando a ms-empleados
 * ({@code GET /api/v1/empleados/me}) con el mismo token. El resultado se guarda en caché
 * por usuario durante un minuto para no repetir la llamada en cada solicitud.
 */
@Component
public class ResolutorContexto {

    private static final Logger LOG = LoggerFactory.getLogger(ResolutorContexto.class);
    private static final Duration VIGENCIA = Duration.ofSeconds(60);

    private final RestClient cliente;
    private final Map<String, Entrada> cache = new ConcurrentHashMap<>();

    public ResolutorContexto(RestClient.Builder builder, @Value("${tj.empleados.url:http://localhost:8081}") String url) {
        this.cliente = builder.baseUrl(url).build();
    }

    public Optional<ContextoUsuario> resolver(Jwt jwt) {
        String oid = Optional.ofNullable(jwt.getClaimAsString("oid")).orElse(jwt.getSubject());
        Entrada guardada = cache.get(oid);
        if (guardada != null && guardada.vence().isAfter(Instant.now())) {
            return Optional.of(guardada.contexto());
        }
        try {
            Perfil p = cliente.get().uri("/api/v1/empleados/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue())
                    .retrieve().body(Perfil.class);
            if (p == null) {
                return Optional.empty();
            }
            ContextoUsuario c = new ContextoUsuario(oid, p.empleadoId(), p.empresaId(), p.sucursalId(), p.equipoId(),
                    p.rol(), p.nombre(), p.correo());
            cache.put(oid, new Entrada(c, Instant.now().plus(VIGENCIA)));
            return Optional.of(c);
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.Unauthorized e) {
            return Optional.empty();
        } catch (RuntimeException e) {
            LOG.error("No se pudo obtener el contexto desde ms-empleados", e);
            throw e;
        }
    }

    /** Respuesta de ms-empleados. */
    record Perfil(Long empleadoId, Long empresaId, Long sucursalId, Long equipoId, Rol rol, String nombre,
                  String correo) {
    }

    private record Entrada(ContextoUsuario contexto, Instant vence) {
    }
}
