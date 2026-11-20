package cl.turnojusto.organizacion.sucursal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.turnojusto.organizacion.comun.ManejadorErrores;
import cl.turnojusto.organizacion.comun.ReglaNegocioException;
import cl.turnojusto.organizacion.seguridad.ConfiguracionSeguridad;
import cl.turnojusto.organizacion.seguridad.ContextoUsuario;
import cl.turnojusto.organizacion.seguridad.ResolutorContexto;
import cl.turnojusto.organizacion.seguridad.Rol;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SucursalController.class)
@Import({ConfiguracionSeguridad.class, ManejadorErrores.class})
class SucursalControllerTest {

    private static final String CUERPO = """
            {"codigo":"MAI","nombre":"Maipú","direccion":"Av. Pajaritos 2500","comuna":"Maipú",
             "regionCodigo":"CL-RM","horaApertura":"10:00","horaCierre":"22:00"}""";

    @Autowired
    MockMvc mvc;
    @MockitoBean
    SucursalService servicio;
    @MockitoBean
    ResolutorContexto resolutor;
    @MockitoBean
    JwtDecoder decoder;

    private void comoRol(Rol rol) {
        when(resolutor.resolver(any())).thenReturn(Optional.of(
                new ContextoUsuario("oid", 1L, 1000L, 1001L, null, rol, "Usuario", "u@x.cl")));
    }

    @Test
    @DisplayName("CP-01: sin token no se accede a la API")
    void sinToken() throws Exception {
        mvc.perform(get("/api/v1/sucursales")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Lista las sucursales visibles")
    void lista() throws Exception {
        comoRol(Rol.COORDINADOR);
        when(servicio.listar(any())).thenReturn(List.of(new SucursalResponse(1001L, 1000L, "CEN", "Centro", "Calle",
                "Santiago", "CL-RM", "09:00", "21:00", "ACTIVA", 2, 0L)));
        mvc.perform(get("/api/v1/sucursales").with(jwt()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].codigo").value("CEN"));
    }

    @Test
    @DisplayName("CP-02: el Coordinador no puede crear sucursales")
    void coordinadorNoCrea() throws Exception {
        comoRol(Rol.COORDINADOR);
        mvc.perform(post("/api/v1/sucursales").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CP-26: horario con formato inválido responde 400")
    void formatoHora() throws Exception {
        comoRol(Rol.ADMINISTRADOR);
        mvc.perform(post("/api/v1/sucursales").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO.replace("22:00", "25:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.horaCierre").value("Usa el formato HH:MM"));
    }

    @Test
    @DisplayName("Una regla de negocio responde 422 con el mensaje en español")
    void reglaNegocio() throws Exception {
        comoRol(Rol.ADMINISTRADOR);
        when(servicio.crear(any(), any()))
                .thenThrow(new ReglaNegocioException("La hora de cierre debe ser posterior a la hora de apertura"));
        mvc.perform(post("/api/v1/sucursales").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("La hora de cierre debe ser posterior a la hora de apertura"));
    }
}
