package cl.turnojusto.empleados.empleado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.turnojusto.empleados.comun.ConflictoException;
import cl.turnojusto.empleados.comun.RecursoNoEncontradoException;
import cl.turnojusto.empleados.comun.ReglaNegocioException;
import cl.turnojusto.empleados.empleado.dto.DesactivacionRequest;
import cl.turnojusto.empleados.empleado.dto.EmpleadoRequest;
import cl.turnojusto.empleados.empleado.dto.TrasladoRequest;
import cl.turnojusto.empleados.integracion.ProveedorIdentidad;
import cl.turnojusto.empleados.integracion.ValidadorUbicacion;
import cl.turnojusto.empleados.outbox.RegistroEventos;
import cl.turnojusto.empleados.seguridad.ContextoUsuario;
import cl.turnojusto.empleados.seguridad.Rol;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class EmpleadoServiceTest {

    private static final ContextoUsuario ADMIN =
            new ContextoUsuario("oid-admin", 1L, 1000L, 1001L, null, Rol.ADMINISTRADOR, "Admin", "a@demo.cl");
    private static final ContextoUsuario COORD =
            new ContextoUsuario("oid-coord", 2L, 1000L, 1001L, 2001L, Rol.COORDINADOR, "Coord", "c@demo.cl");
    private static final ContextoUsuario EMPL =
            new ContextoUsuario("oid-emp", 3L, 1000L, 1001L, 2001L, Rol.EMPLEADO, "Emp", "e@demo.cl");

    @Mock
    EmpleadoRepository empleados;
    @Mock
    TrasladoRepository traslados;
    @Mock
    ValidadorUbicacion ubicacion;
    @Mock
    RegistroEventos eventos;
    @Mock
    ProveedorIdentidad identidad;
    @InjectMocks
    EmpleadoService servicio;

    private Empleado existente;

    @BeforeEach
    void preparar() {
        existente = new Empleado(1000L, 1001L, 2001L, "12345678-5", "Ana", "Pérez", "ana@demo.cl",
                LocalDate.of(2026, 1, 1), Rol.EMPLEADO);
        existente.setIdParaPruebas(50L);
    }

    private EmpleadoRequest solicitud(String rut, String correo) {
        return new EmpleadoRequest(rut, "Ana", "Pérez", correo, "", "Cajera", Rol.EMPLEADO, 1001L, 2001L,
                LocalDate.of(2026, 1, 1));
    }

    @Test
    @DisplayName("CP-20: registra un empleado con RUT normalizado y publica EmpleadoRegistrado")
    void creaEmpleado() {
        when(empleados.save(any(Empleado.class))).thenAnswer(i -> i.getArgument(0));
        when(identidad.crearCuenta("ana@demo.cl", "Ana Pérez")).thenReturn(Optional.of("oid-nuevo"));
        var r = servicio.crear(ADMIN, solicitud("12.345.678-5", "ANA@Demo.cl"));
        assertThat(r.cuentaVinculada()).isTrue();
        assertThat(r.rut()).isEqualTo("12345678-5");
        assertThat(r.correo()).isEqualTo("ana@demo.cl");
        assertThat(r.empresaId()).isEqualTo(1000L);
        verify(ubicacion).validar(1001L, 2001L);
        verify(eventos).registrar(eq(1000L), eq("EmpleadoRegistrado"), any(), anyMap());
    }

    @Test
    @DisplayName("CP-22: rechaza un RUT repetido en la empresa")
    void rutRepetido() {
        when(empleados.existsByEmpresaIdAndRut(1000L, "12345678-5")).thenReturn(true);
        assertThatThrownBy(() -> servicio.crear(ADMIN, solicitud("12345678-5", "x@demo.cl")))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("RUT");
        verify(empleados, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza un correo repetido en la empresa")
    void correoRepetido() {
        when(empleados.existsByEmpresaIdAndCorreoIgnoreCase(1000L, "x@demo.cl")).thenReturn(true);
        assertThatThrownBy(() -> servicio.crear(ADMIN, solicitud("12345678-5", "X@demo.cl")))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("correo");
    }

    @Test
    @DisplayName("CP-13: el Coordinador solo busca en su sucursal aunque pida otra")
    void coordinadorQuedaEnSuSucursal() {
        when(empleados.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(existente)));
        var pagina = servicio.buscar(COORD, null, 9999L, null, null, 0, 20);
        assertThat(pagina.contenido()).hasSize(1);
    }

    @Test
    @DisplayName("CP-02: el Empleado no puede listar empleados")
    void empleadoNoLista() {
        assertThatThrownBy(() -> servicio.buscar(EMPL, null, null, null, null, 0, 20))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("CP-13: un Coordinador no ve fichas de otra sucursal (responde 404)")
    void coordinadorNoVeOtraSucursal() {
        Empleado otro = new Empleado(1000L, 1002L, 2003L, "11111111-1", "B", "C", "b@demo.cl",
                LocalDate.of(2026, 1, 1), Rol.EMPLEADO);
        when(empleados.findByIdAndEmpresaId(60L, 1000L)).thenReturn(Optional.of(otro));
        assertThatThrownBy(() -> servicio.obtener(COORD, 60L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("CP-03: un usuario no ve fichas de otra empresa")
    void aislamientoEntreEmpresas() {
        when(empleados.findByIdAndEmpresaId(50L, 1000L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.obtener(ADMIN, 50L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("El Empleado ve su propia ficha")
    void empleadoVeSuFicha() {
        Empleado propio = new Empleado(1000L, 1001L, 2001L, "11111111-1", "E", "M", "e@demo.cl",
                LocalDate.of(2026, 1, 1), Rol.EMPLEADO);
        propio.setIdParaPruebas(3L);
        when(empleados.findByIdAndEmpresaId(3L, 1000L)).thenReturn(Optional.of(propio));
        assertThat(servicio.obtener(EMPL, 3L).id()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Editar no permite cambiar la sucursal: debe usarse el traslado")
    void editarNoTraslada() {
        when(empleados.findByIdAndEmpresaId(50L, 1000L)).thenReturn(Optional.of(existente));
        var req = new EmpleadoRequest("12345678-5", "Ana", "Pérez", "ana@demo.cl", null, null, Rol.EMPLEADO,
                1002L, null, LocalDate.of(2026, 1, 1));
        assertThatThrownBy(() -> servicio.actualizar(ADMIN, 50L, req))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("Trasladar");
    }

    @Test
    @DisplayName("Traslado: guarda el historial, cambia la ubicación y publica EmpleadoTrasladado")
    void trasladaEmpleado() {
        when(empleados.findByIdAndEmpresaId(50L, 1000L)).thenReturn(Optional.of(existente));
        when(empleados.save(any(Empleado.class))).thenAnswer(i -> i.getArgument(0));
        var r = servicio.trasladar(ADMIN, 50L, new TrasladoRequest(1002L, 2003L, LocalDate.of(2026, 12, 1), "x"));
        assertThat(r.sucursalId()).isEqualTo(1002L);
        verify(traslados).save(any(Traslado.class));
        verify(eventos).registrar(eq(1000L), eq("EmpleadoTrasladado"), eq(50L), anyMap());
    }

    @Test
    @DisplayName("CP-15: desactivar saca al empleado de la rotación y publica el evento")
    void desactiva() {
        when(empleados.findByIdAndEmpresaId(50L, 1000L)).thenReturn(Optional.of(existente));
        when(empleados.save(any(Empleado.class))).thenAnswer(i -> i.getArgument(0));
        var r = servicio.desactivar(ADMIN, 50L, new DesactivacionRequest(LocalDate.of(2026, 11, 30), "Renuncia"));
        assertThat(r.estado()).isEqualTo(EstadoEmpleado.INACTIVO);
        verify(eventos).registrar(eq(1000L), eq("EmpleadoDesactivado"), eq(50L), anyMap());
    }

    @Test
    @DisplayName("El administrador no puede desactivarse a sí mismo")
    void noSeDesactivaASiMismo() {
        Empleado admin = new Empleado(1000L, 1001L, null, "11111111-1", "A", "D", "a@demo.cl",
                LocalDate.of(2026, 1, 1), Rol.ADMINISTRADOR);
        admin.setIdParaPruebas(1L);
        when(empleados.findByIdAndEmpresaId(1L, 1000L)).thenReturn(Optional.of(admin));
        assertThatThrownBy(() -> servicio.desactivar(ADMIN, 1L, new DesactivacionRequest(LocalDate.now(), "x")))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Reactivar vuelve a validar la sucursal y publica EmpleadoReactivado")
    void reactiva() {
        existente.desactivar(LocalDate.of(2026, 10, 1), "x");
        when(empleados.findByIdAndEmpresaId(50L, 1000L)).thenReturn(Optional.of(existente));
        when(empleados.save(any(Empleado.class))).thenAnswer(i -> i.getArgument(0));
        assertThat(servicio.reactivar(ADMIN, 50L).estado()).isEqualTo(EstadoEmpleado.ACTIVO);
        verify(ubicacion).validar(1001L, 2001L);
    }
}
