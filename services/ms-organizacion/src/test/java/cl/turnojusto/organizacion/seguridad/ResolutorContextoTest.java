package cl.turnojusto.organizacion.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ResolutorContextoTest {

    private static Jwt token(String oid) {
        return Jwt.withTokenValue("tk-" + oid).header("alg", "none").claim("oid", oid)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    }

    @Test
    void consultaMsEmpleadosUnaVezYUsaLaCache() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(once(), requestTo("http://emp/api/v1/empleados/me"))
                .andExpect(header("Authorization", "Bearer tk-a"))
                .andRespond(withSuccess("""
                        {"empleadoId":1,"empresaId":1000,"sucursalId":1001,"rol":"ADMINISTRADOR","nombre":"A"}""",
                        MediaType.APPLICATION_JSON));
        ResolutorContexto r = new ResolutorContexto(builder, "http://emp");
        assertThat(r.resolver(token("a"))).get().extracting(ContextoUsuario::empresaId).isEqualTo(1000L);
        assertThat(r.resolver(token("a"))).isPresent();
        servidor.verify();
    }

    @Test
    void sinFichaNoHayContexto() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://emp/api/v1/empleados/me")).andRespond(withStatus(HttpStatus.FORBIDDEN));
        assertThat(new ResolutorContexto(builder, "http://emp").resolver(token("b"))).isEmpty();
    }
}
