package cl.turnojusto.empleados.integracion;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import cl.turnojusto.empleados.comun.ReglaNegocioException;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OrganizacionClientTest {

    private MockRestServiceServer servidor;
    private OrganizacionClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new OrganizacionClient(builder, "http://org");
        Jwt jwt = Jwt.withTokenValue("token-usuario").header("alg", "none").claim("oid", "x")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validaSucursalYEquipoReenviandoElToken() {
        servidor.expect(requestTo("http://org/api/v1/sucursales/1001"))
                .andExpect(header("Authorization", "Bearer token-usuario"))
                .andRespond(withSuccess("{\"id\":1001,\"estado\":\"ACTIVA\"}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo("http://org/api/v1/equipos/2001"))
                .andRespond(withSuccess("{\"id\":2001,\"sucursalId\":1001,\"estado\":\"ACTIVO\"}",
                        MediaType.APPLICATION_JSON));
        assertThatCode(() -> cliente.validar(1001L, 2001L)).doesNotThrowAnyException();
        servidor.verify();
    }

    @Test
    void sucursalInexistente() {
        servidor.expect(requestTo("http://org/api/v1/sucursales/9")).andRespond(withResourceNotFound());
        assertThatThrownBy(() -> cliente.validar(9L, null))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("La sucursal no existe");
    }

    @Test
    void equipoDeOtraSucursal() {
        servidor.expect(requestTo("http://org/api/v1/sucursales/1001"))
                .andRespond(withSuccess("{\"estado\":\"ACTIVA\"}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo("http://org/api/v1/equipos/2003"))
                .andRespond(withSuccess("{\"sucursalId\":1002,\"estado\":\"ACTIVO\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> cliente.validar(1001L, 2003L))
                .hasMessage("El equipo no pertenece a la sucursal indicada");
    }

    @Test
    void sucursalInactiva() {
        servidor.expect(requestTo("http://org/api/v1/sucursales/1001"))
                .andRespond(withSuccess("{\"estado\":\"INACTIVA\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> cliente.validar(1001L, null)).hasMessage("La sucursal está inactiva");
    }

    @Test
    void sinUrlNoValida() {
        assertThatCode(() -> new OrganizacionClient(RestClient.builder(), "").validar(1L, 2L))
                .doesNotThrowAnyException();
    }
}
