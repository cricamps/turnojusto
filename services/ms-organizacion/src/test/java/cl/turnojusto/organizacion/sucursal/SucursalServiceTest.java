package cl.turnojusto.organizacion.sucursal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.turnojusto.organizacion.comun.ConflictoException;
import cl.turnojusto.organizacion.comun.RecursoNoEncontradoException;
import cl.turnojusto.organizacion.comun.ReglaNegocioException;
import cl.turnojusto.organizacion.equipo.EquipoRepository;
import cl.turnojusto.organizacion.region.RegionRepository;
import cl.turnojusto.organizacion.seguridad.ContextoUsuario;
import cl.turnojusto.organizacion.seguridad.Rol;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SucursalServiceTest {

    private static final ContextoUsuario ADMIN =
            new ContextoUsuario("oid", 1L, 1000L, 1001L, null, Rol.ADMINISTRADOR, "Admin", "a@x.cl");
    private static final ContextoUsuario COORD =
            new ContextoUsuario("oid", 2L, 1000L, 1001L, 2001L, Rol.COORDINADOR, "Coord", "c@x.cl");

    @Mock
    SucursalRepository sucursales;
    @Mock
    EquipoRepository equipos;
    @Mock
    RegionRepository regiones;
    @InjectMocks
    SucursalService servicio;

    private static SucursalRequest req(String codigo, String apertura, String cierre) {
        return new SucursalRequest(codigo, "Maipú", "Av. Pajaritos 2500", "Maipú", "CL-RM", apertura, cierre);
    }

    private static Sucursal sucursal(long id) {
        Sucursal s = new Sucursal(1000L);
        s.definir("S" + id, "Sucursal " + id, "Calle 1", "Santiago", "CL-RM", "09:00", "18:00");
        s.setIdParaPruebas(id);
        return s;
    }

    @Test
    @DisplayName("CP-17: el Administrador registra una sucursal con código en mayúsculas")
    void crea() {
        when(regiones.existsById("CL-RM")).thenReturn(true);
        when(sucursales.save(any(Sucursal.class))).thenAnswer(i -> i.getArgument(0));
        SucursalResponse r = servicio.crear(ADMIN, req("mai", "10:00", "22:00"));
        assertThat(r.codigo()).isEqualTo("MAI");
        assertThat(r.empresaId()).isEqualTo(1000L);
        assertThat(r.estado()).isEqualTo("ACTIVA");
    }

    @Test
    @DisplayName("CP-26: rechaza un horario con cierre anterior a la apertura")
    void horarioInvalido() {
        when(regiones.existsById("CL-RM")).thenReturn(true);
        assertThatThrownBy(() -> servicio.crear(ADMIN, req("MAI", "22:00", "10:00")))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("cierre");
        verify(sucursales, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza un código de sucursal repetido en la empresa")
    void codigoRepetido() {
        when(sucursales.existsByEmpresaIdAndCodigoIgnoreCase(1000L, "MAI")).thenReturn(true);
        assertThatThrownBy(() -> servicio.crear(ADMIN, req("MAI", "10:00", "22:00")))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    @DisplayName("Rechaza una región inexistente")
    void regionInexistente() {
        when(regiones.existsById("CL-RM")).thenReturn(false);
        assertThatThrownBy(() -> servicio.crear(ADMIN, req("MAI", "10:00", "22:00")))
                .hasMessage("La región no existe");
    }

    @Test
    @DisplayName("CP-13: el Coordinador solo ve su sucursal")
    void coordinadorVeSoloLaSuya() {
        when(sucursales.findByEmpresaIdOrderByNombreAsc(1000L)).thenReturn(List.of(sucursal(1001), sucursal(1002)));
        assertThat(servicio.listar(COORD)).extracting(SucursalResponse::id).containsExactly(1001L);
        assertThat(servicio.listar(ADMIN)).hasSize(2);
    }

    @Test
    @DisplayName("CP-13: el Coordinador no puede abrir otra sucursal (404)")
    void coordinadorNoAbreOtra() {
        when(sucursales.findByIdAndEmpresaId(1002L, 1000L)).thenReturn(Optional.of(sucursal(1002)));
        assertThatThrownBy(() -> servicio.obtener(COORD, 1002L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("No se desactiva una sucursal con equipos activos")
    void noDesactivaConEquipos() {
        when(sucursales.findByIdAndEmpresaId(1001L, 1000L)).thenReturn(Optional.of(sucursal(1001)));
        when(equipos.countBySucursalIdAndEstado(1001L, "ACTIVO")).thenReturn(2L);
        assertThatThrownBy(() -> servicio.cambiarEstado(ADMIN, 1001L, false))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Edita una sucursal sin chocar consigo misma")
    void edita() {
        Sucursal s = sucursal(1001);
        when(sucursales.findByIdAndEmpresaId(1001L, 1000L)).thenReturn(Optional.of(s));
        when(regiones.existsById("CL-RM")).thenReturn(true);
        when(sucursales.save(any(Sucursal.class))).thenAnswer(i -> i.getArgument(0));
        SucursalResponse r = servicio.actualizar(ADMIN, 1001L, req("CEN", "08:30", "20:00"));
        assertThat(r.horaApertura()).isEqualTo("08:30");
    }
}
