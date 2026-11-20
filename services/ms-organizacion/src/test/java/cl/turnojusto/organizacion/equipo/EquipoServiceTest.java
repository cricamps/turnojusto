package cl.turnojusto.organizacion.equipo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.turnojusto.organizacion.comun.ConflictoException;
import cl.turnojusto.organizacion.comun.ReglaNegocioException;
import cl.turnojusto.organizacion.equipo.EquipoDtos.EquipoRequest;
import cl.turnojusto.organizacion.integracion.ConsultaEmpleados;
import cl.turnojusto.organizacion.integracion.ConsultaEmpleados.EmpleadoResumen;
import cl.turnojusto.organizacion.seguridad.ContextoUsuario;
import cl.turnojusto.organizacion.seguridad.Rol;
import cl.turnojusto.organizacion.sucursal.Sucursal;
import cl.turnojusto.organizacion.sucursal.SucursalRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EquipoServiceTest {

    private static final ContextoUsuario ADMIN =
            new ContextoUsuario("oid", 1L, 1000L, 1001L, null, Rol.ADMINISTRADOR, "Admin", "a@x.cl");
    private static final ContextoUsuario COORD =
            new ContextoUsuario("oid", 2L, 1000L, 1001L, 2001L, Rol.COORDINADOR, "Coord", "c@x.cl");

    @Mock
    EquipoRepository equipos;
    @Mock
    CoordinadorEquipoRepository coordinaciones;
    @Mock
    SucursalRepository sucursales;
    @Mock
    ConsultaEmpleados empleados;

    private EquipoService servicio;
    private Equipo cajas;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-20T12:00:00Z"), ZoneId.of("America/Santiago"));
        servicio = new EquipoService(equipos, coordinaciones, sucursales, empleados, Optional.of(reloj));
        cajas = new Equipo(1000L, 1001L, "Cajas", null);
        cajas.setIdParaPruebas(2001L);
    }

    private static Sucursal sucursalActiva() {
        Sucursal s = new Sucursal(1000L);
        s.definir("CEN", "Centro", "Calle 1", "Santiago", "CL-RM", "09:00", "21:00");
        return s;
    }

    @Test
    @DisplayName("CP-17: crea un equipo en una sucursal activa de la empresa")
    void crea() {
        when(sucursales.findByIdAndEmpresaId(1001L, 1000L)).thenReturn(Optional.of(sucursalActiva()));
        when(equipos.save(any(Equipo.class))).thenAnswer(i -> i.getArgument(0));
        var r = servicio.crear(ADMIN, new EquipoRequest(1001L, " Bodega ", ""));
        assertThat(r.nombre()).isEqualTo("Bodega");
        assertThat(r.descripcion()).isNull();
    }

    @Test
    @DisplayName("CP-18: no se duplica un equipo en la misma sucursal")
    void duplicado() {
        Sucursal s = sucursalActiva();
        when(sucursales.findByIdAndEmpresaId(1001L, 1000L)).thenReturn(Optional.of(s));
        when(equipos.existsBySucursalIdAndNombreIgnoreCase(s.getId(), "Cajas")).thenReturn(true);
        assertThatThrownBy(() -> servicio.crear(ADMIN, new EquipoRequest(1001L, "Cajas", null)))
                .isInstanceOf(ConflictoException.class);
        verify(equipos, never()).save(any());
    }

    @Test
    @DisplayName("No crea equipos en una sucursal inactiva")
    void sucursalInactiva() {
        Sucursal s = sucursalActiva();
        s.cambiarEstado(false);
        when(sucursales.findByIdAndEmpresaId(1001L, 1000L)).thenReturn(Optional.of(s));
        assertThatThrownBy(() -> servicio.crear(ADMIN, new EquipoRequest(1001L, "X", null)))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("CP-19: asigna coordinador y cierra la vigencia del anterior")
    void asignaCoordinador() {
        CoordinadorEquipo anterior = new CoordinadorEquipo(1000L, 2001L, 3001L, LocalDate.of(2026, 1, 1));
        when(equipos.findByIdAndEmpresaId(2001L, 1000L)).thenReturn(Optional.of(cajas));
        when(empleados.buscar(3009L)).thenReturn(Optional.of(new EmpleadoResumen(3009L, 1001L, "COORDINADOR", "ACTIVO")));
        when(coordinaciones.findByEquipoIdAndVigenteHastaIsNull(2001L)).thenReturn(Optional.of(anterior));
        when(coordinaciones.save(any(CoordinadorEquipo.class))).thenAnswer(i -> i.getArgument(0));
        var r = servicio.asignarCoordinador(ADMIN, 2001L, 3009L);
        assertThat(r.coordinadorEmpleadoId()).isEqualTo(3009L);
        assertThat(r.coordinadorDesde()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(anterior.getVigenteHasta()).isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @Test
    @DisplayName("CP-19: el coordinador debe ser de la misma sucursal")
    void coordinadorDeOtraSucursal() {
        when(equipos.findByIdAndEmpresaId(2001L, 1000L)).thenReturn(Optional.of(cajas));
        when(empleados.buscar(3009L)).thenReturn(Optional.of(new EmpleadoResumen(3009L, 1002L, "COORDINADOR", "ACTIVO")));
        assertThatThrownBy(() -> servicio.asignarCoordinador(ADMIN, 2001L, 3009L))
                .hasMessageContaining("sucursal del equipo");
    }

    @Test
    @DisplayName("CP-19: un empleado con rol Empleado no puede coordinar")
    void rolEmpleadoNoCoordina() {
        when(equipos.findByIdAndEmpresaId(2001L, 1000L)).thenReturn(Optional.of(cajas));
        when(empleados.buscar(3002L)).thenReturn(Optional.of(new EmpleadoResumen(3002L, 1001L, "EMPLEADO", "ACTIVO")));
        assertThatThrownBy(() -> servicio.asignarCoordinador(ADMIN, 2001L, 3002L))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("rol");
    }

    @Test
    @DisplayName("Un empleado inactivo no puede coordinar")
    void inactivoNoCoordina() {
        when(equipos.findByIdAndEmpresaId(2001L, 1000L)).thenReturn(Optional.of(cajas));
        when(empleados.buscar(3017L)).thenReturn(Optional.of(new EmpleadoResumen(3017L, 1001L, "COORDINADOR", "INACTIVO")));
        assertThatThrownBy(() -> servicio.asignarCoordinador(ADMIN, 2001L, 3017L)).hasMessageContaining("inactivo");
    }

    @Test
    @DisplayName("CP-13: el Coordinador solo lista los equipos de su sucursal")
    void coordinadorListaSuSucursal() {
        when(equipos.findByEmpresaIdAndSucursalIdOrderByNombreAsc(1000L, 1001L)).thenReturn(List.of(cajas));
        when(coordinaciones.findByEmpresaIdAndVigenteHastaIsNull(1000L)).thenReturn(List.of());
        assertThat(servicio.listar(COORD, 1002L)).hasSize(1);
        verify(equipos).findByEmpresaIdAndSucursalIdOrderByNombreAsc(1000L, 1001L);
    }

    @Test
    @DisplayName("Un equipo no se mueve de sucursal al editar")
    void noMueveDeSucursal() {
        when(equipos.findByIdAndEmpresaId(2001L, 1000L)).thenReturn(Optional.of(cajas));
        assertThatThrownBy(() -> servicio.actualizar(ADMIN, 2001L, new EquipoRequest(1002L, "Cajas", null)))
                .isInstanceOf(ReglaNegocioException.class);
    }
}
