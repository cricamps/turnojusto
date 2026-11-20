package cl.turnojusto.organizacion.equipo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Objetos de entrada y salida de la API de equipos. */
public final class EquipoDtos {

    private EquipoDtos() {
    }

    /** Datos del formulario de equipo. */
    public record EquipoRequest(
            @NotNull(message = "La sucursal es obligatoria") Long sucursalId,
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) String nombre,
            @Size(max = 300) String descripcion) {
    }

    /** Equipo con su coordinador vigente. */
    public record EquipoResponse(Long id, Long sucursalId, String nombre, String descripcion, String estado,
                                 Long coordinadorEmpleadoId, LocalDate coordinadorDesde, Long version) {
    }

    /** Asignación de coordinador. */
    public record CoordinadorRequest(@NotNull(message = "El empleado es obligatorio") Long empleadoId) {
    }

    /** Fila del historial de coordinadores. */
    public record CoordinacionResponse(Long empleadoId, LocalDate desde, LocalDate hasta) {
    }
}
