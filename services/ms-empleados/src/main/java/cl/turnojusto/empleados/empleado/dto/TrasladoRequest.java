package cl.turnojusto.empleados.empleado.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Solicitud de traslado a otra sucursal o equipo. */
public record TrasladoRequest(
        @NotNull(message = "La sucursal de destino es obligatoria") Long sucursalDestinoId,
        Long equipoDestinoId,
        @NotNull(message = "La fecha efectiva es obligatoria") LocalDate fechaEfectiva,
        @Size(max = 200) String motivo) {
}
