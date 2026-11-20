package cl.turnojusto.organizacion.integracion;

import cl.turnojusto.organizacion.comun.ReglaNegocioException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/** Adaptador REST hacia ms-empleados con reenvío del token del usuario. */
@Component
public class EmpleadosClient implements ConsultaEmpleados {

    private final RestClient cliente;

    public EmpleadosClient(RestClient.Builder builder, @Value("${tj.empleados.url:http://localhost:8081}") String url) {
        this.cliente = builder.baseUrl(url).build();
    }

    @Override
    public Optional<EmpleadoResumen> buscar(Long empleadoId) {
        try {
            return Optional.ofNullable(cliente.get().uri("/api/v1/empleados/{id}", empleadoId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                    .retrieve().body(EmpleadoResumen.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (HttpServerErrorException | ResourceAccessException e) {
            throw new ReglaNegocioException("ms-empleados no está disponible; intenta nuevamente");
        }
    }

    private static String token() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken t) {
            return t.getToken().getTokenValue();
        }
        throw new IllegalStateException("No hay token para llamar a ms-empleados");
    }
}
