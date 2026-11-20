package cl.turnojusto.empleados.integracion;

import cl.turnojusto.empleados.comun.ReglaNegocioException;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Adaptador REST hacia ms-organizacion. Reenvía el mismo token del usuario (token relay),
 * de modo que ms-organizacion aplica sus propias reglas de empresa y sucursal.
 */
@Component
public class OrganizacionClient implements ValidadorUbicacion {

    private static final Logger LOG = LoggerFactory.getLogger(OrganizacionClient.class);

    private final RestClient cliente;
    private final boolean habilitado;

    public OrganizacionClient(RestClient.Builder builder, @Value("${tj.organizacion.url:}") String url) {
        this.habilitado = url != null && !url.isBlank();
        this.cliente = builder.baseUrl(habilitado ? url : "http://localhost").build();
        if (!habilitado) {
            LOG.warn("tj.organizacion.url no está configurada: no se validarán sucursales ni equipos");
        }
    }

    @Override
    public void validar(Long sucursalId, Long equipoId) {
        if (!habilitado) {
            return;
        }
        Map<?, ?> sucursal = obtener("/api/v1/sucursales/{id}", sucursalId, "La sucursal no existe");
        if (!"ACTIVA".equals(sucursal.get("estado"))) {
            throw new ReglaNegocioException("La sucursal está inactiva");
        }
        if (equipoId != null) {
            Map<?, ?> equipo = obtener("/api/v1/equipos/{id}", equipoId, "El equipo no existe");
            Object sucursalDelEquipo = equipo.get("sucursalId");
            if (sucursalDelEquipo == null || !Objects.equals(sucursalId, ((Number) sucursalDelEquipo).longValue())) {
                throw new ReglaNegocioException("El equipo no pertenece a la sucursal indicada");
            }
            if (!"ACTIVO".equals(equipo.get("estado"))) {
                throw new ReglaNegocioException("El equipo está inactivo");
            }
        }
    }

    private Map<?, ?> obtener(String ruta, Long id, String mensajeNoExiste) {
        try {
            return cliente.get().uri(ruta, id)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenActual())
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new ReglaNegocioException("ms-organizacion no está disponible; intenta nuevamente");
                    })
                    .body(Map.class);
        } catch (HttpClientErrorException.NotFound | HttpClientErrorException.Forbidden e) {
            throw new ReglaNegocioException(mensajeNoExiste);
        }
    }

    private static String tokenActual() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken t) {
            return t.getToken().getTokenValue();
        }
        throw new IllegalStateException("No hay token para llamar a ms-organizacion");
    }
}
