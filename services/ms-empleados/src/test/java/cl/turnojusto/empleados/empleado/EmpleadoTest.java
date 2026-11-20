package cl.turnojusto.empleados.empleado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.turnojusto.empleados.comun.ReglaNegocioException;
import cl.turnojusto.empleados.seguridad.Rol;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EmpleadoTest {

    private Empleado nuevo() {
        return new Empleado(1L, 10L, 100L, "12345678-5", "Ana", "Pérez", "ana@demo.cl",
                LocalDate.of(2026, 1, 1), Rol.EMPLEADO);
    }

    @Test
    void desactivarGuardaFechaYMotivo() {
        Empleado e = nuevo();
        e.desactivar(LocalDate.of(2026, 11, 1), "Renuncia");
        assertThat(e.estaActivo()).isFalse();
        assertThat(e.getFechaDesactivacion()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(e.getMotivoDesactivacion()).isEqualTo("Renuncia");
    }

    @Test
    void noSeDesactivaAntesDelIngreso() {
        Empleado e = nuevo();
        assertThatThrownBy(() -> e.desactivar(LocalDate.of(2025, 12, 31), "x"))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void noSeDesactivaDosVeces() {
        Empleado e = nuevo();
        e.desactivar(LocalDate.of(2026, 11, 1), "x");
        assertThatThrownBy(() -> e.desactivar(LocalDate.of(2026, 11, 2), "y"))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("ya está inactivo");
    }

    @Test
    void reactivarLimpiaLaDesactivacion() {
        Empleado e = nuevo();
        e.desactivar(LocalDate.of(2026, 11, 1), "x");
        e.reactivar();
        assertThat(e.estaActivo()).isTrue();
        assertThat(e.getFechaDesactivacion()).isNull();
    }

    @Test
    void trasladoCambiaUbicacion() {
        Empleado e = nuevo();
        e.trasladar(20L, null);
        assertThat(e.getSucursalId()).isEqualTo(20L);
        assertThat(e.getEquipoId()).isNull();
    }

    @Test
    void trasladoAlMismoLugarSeRechaza() {
        Empleado e = nuevo();
        assertThatThrownBy(() -> e.trasladar(10L, 100L)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void inactivoNoSeTraslada() {
        Empleado e = nuevo();
        e.desactivar(LocalDate.of(2026, 11, 1), "x");
        assertThatThrownBy(() -> e.trasladar(20L, null)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void auditoriaRegistraCreadorYLuegoEditor() {
        Empleado e = nuevo();
        e.auditar("oid-1");
        e.auditar("oid-2");
        // El creador no cambia al editar; el editor queda en ACTUALIZADO_POR.
        assertThat(e).extracting("creadoPor", "actualizadoPor").containsExactly("oid-1", "oid-2");
    }
}
