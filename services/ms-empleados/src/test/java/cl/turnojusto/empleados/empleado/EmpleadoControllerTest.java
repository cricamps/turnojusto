package cl.turnojusto.empleados.empleado;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.turnojusto.empleados.empleado.dto.EmpleadoResponse;
import cl.turnojusto.empleados.seguridad.ConfiguracionSeguridad;
import cl.turnojusto.empleados.seguridad.ContextoUsuario;
import cl.turnojusto.empleados.seguridad.ResolutorContexto;
import cl.turnojusto.empleados.seguridad.Rol;
import cl.turnojusto.empleados.comun.ManejadorErrores;
import java.time.LocalDate;
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

@WebMvcTest(EmpleadoController.class)
@Import({ConfiguracionSeguridad.class, ManejadorErrores.class})
class EmpleadoControllerTest {

    @Autowired
    MockMvc mvc;
    @MockitoBean
    EmpleadoService servicio;
    @MockitoBean
    ResolutorContexto resolutor;
    @MockitoBean
    JwtDecoder decoder;

    private void comoRol(Rol rol) {
        when(resolutor.resolver(any())).thenReturn(Optional.of(
                new ContextoUsuario("oid", 1L, 1000L, 1001L, null, rol, "Andrea Muñoz", "a@demo.cl")));
    }

    @Test
    @DisplayName("Sin token responde 401")
    void sinToken() throws Exception {
        mvc.perform(get("/api/v1/empleados/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token válido pero sin ficha responde 403 con mensaje claro")
    void sinFicha() throws Exception {
        when(resolutor.resolver(any())).thenReturn(Optional.empty());
        mvc.perform(get("/api/v1/empleados/me").with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Sin acceso"));
    }

    @Test
    @DisplayName("GET /me devuelve el contexto del usuario")
    void perfil() throws Exception {
        comoRol(Rol.ADMINISTRADOR);
        mvc.perform(get("/api/v1/empleados/me").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresaId").value(1000))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("CP-02: un Empleado no puede registrar empleados (403)")
    void empleadoNoCrea() throws Exception {
        comoRol(Rol.EMPLEADO);
        mvc.perform(post("/api/v1/empleados").with(jwt()).contentType(MediaType.APPLICATION_JSON).content("""
                {"rut":"12345678-5","nombres":"A","apellidos":"B","correo":"a@b.cl","rol":"EMPLEADO",
                 "sucursalId":1001,"fechaIngresoRotacion":"2026-01-01"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CP-21: campos obligatorios y RUT inválido responden 400 con el detalle por campo")
    void validaCampos() throws Exception {
        comoRol(Rol.ADMINISTRADOR);
        mvc.perform(post("/api/v1/empleados").with(jwt()).contentType(MediaType.APPLICATION_JSON).content("""
                {"rut":"12345678-9","nombres":"","apellidos":"B","correo":"no-es-correo","rol":"EMPLEADO",
                 "sucursalId":1001,"fechaIngresoRotacion":"2026-01-01"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.rut").value("El RUT no es válido"))
                .andExpect(jsonPath("$.campos.nombres").exists())
                .andExpect(jsonPath("$.campos.correo").exists());
    }

    @Test
    @DisplayName("CP-20: el Administrador registra un empleado (201)")
    void adminCrea() throws Exception {
        comoRol(Rol.ADMINISTRADOR);
        when(servicio.crear(any(), any())).thenReturn(new EmpleadoResponse(77L, 1000L, 1001L, null,
                "12345678-5", "A", "B", "a@b.cl", null, null, Rol.EMPLEADO, LocalDate.of(2026, 1, 1),
                EstadoEmpleado.ACTIVO, null, null, false, 0L));
        mvc.perform(post("/api/v1/empleados").with(jwt()).contentType(MediaType.APPLICATION_JSON).content("""
                {"rut":"12.345.678-5","nombres":"A","apellidos":"B","correo":"a@b.cl","rol":"EMPLEADO",
                 "sucursalId":1001,"fechaIngresoRotacion":"2026-01-01"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(77));
    }

    @Test
    @DisplayName("El health check es público para el balanceador")
    void healthPublico() throws Exception {
        // Actuator no se carga en @WebMvcTest; basta con verificar que la ruta no exige token.
        mvc.perform(get("/actuator/health")).andExpect(status().isNotFound());
    }
}
