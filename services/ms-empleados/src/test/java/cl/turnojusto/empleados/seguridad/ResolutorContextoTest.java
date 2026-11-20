package cl.turnojusto.empleados.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.turnojusto.empleados.empleado.Empleado;
import cl.turnojusto.empleados.empleado.EmpleadoRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class ResolutorContextoTest {

    @Mock
    EmpleadoRepository repositorio;
    @InjectMocks
    ResolutorContexto resolutor;

    private static Jwt token(String oid, String email) {
        return Jwt.withTokenValue("t").header("alg", "none").claim("oid", oid).claim("email", email)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    }

    private static Empleado ficha(Rol rol) {
        return new Empleado(1000L, 1001L, null, "11111111-1", "Andrea", "Muñoz", "admin.demo@camps.cl",
                LocalDate.of(2026, 1, 1), rol);
    }

    @Test
    @DisplayName("CP-01: resuelve empresa, sucursal y rol por el oid del token")
    void resuelvePorOid() {
        when(repositorio.findByUsuarioOid("oid-1")).thenReturn(Optional.of(ficha(Rol.ADMINISTRADOR)));
        var ctx = resolutor.resolver(token("oid-1", "admin.demo@camps.cl")).orElseThrow();
        assertThat(ctx.empresaId()).isEqualTo(1000L);
        assertThat(ctx.rol()).isEqualTo(Rol.ADMINISTRADOR);
        assertThat(ctx.nombre()).isEqualTo("Andrea Muñoz");
    }

    @Test
    @DisplayName("Primer ingreso: vincula la cuenta de Entra a la ficha con el mismo correo")
    void vinculaPorCorreo() {
        Empleado e = ficha(Rol.COORDINADOR);
        when(repositorio.findByUsuarioOid("oid-2")).thenReturn(Optional.empty());
        when(repositorio.findByCorreoIgnoreCaseAndUsuarioOidIsNull("admin.demo@camps.cl")).thenReturn(List.of(e));
        when(repositorio.save(any(Empleado.class))).thenAnswer(i -> i.getArgument(0));
        var ctx = resolutor.resolver(token("oid-2", "Admin.Demo@camps.cl"));
        assertThat(ctx).isPresent();
        assertThat(e.getUsuarioOid()).isEqualTo("oid-2");
        verify(repositorio).save(e);
    }

    @Test
    @DisplayName("Sin ficha no hay acceso")
    void sinFicha() {
        when(repositorio.findByUsuarioOid("oid-3")).thenReturn(Optional.empty());
        when(repositorio.findByCorreoIgnoreCaseAndUsuarioOidIsNull("nadie@x.cl")).thenReturn(List.of());
        assertThat(resolutor.resolver(token("oid-3", "nadie@x.cl"))).isEmpty();
    }

    @Test
    @DisplayName("CP-14: un empleado desactivado no obtiene acceso")
    void inactivoSinAcceso() {
        Empleado e = ficha(Rol.EMPLEADO);
        e.desactivar(LocalDate.of(2026, 11, 1), "x");
        when(repositorio.findByUsuarioOid("oid-4")).thenReturn(Optional.of(e));
        assertThat(resolutor.resolver(token("oid-4", "x@x.cl"))).isEmpty();
    }
}
