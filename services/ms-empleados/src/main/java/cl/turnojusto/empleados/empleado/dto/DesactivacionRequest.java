package cl.turnojusto.empleados.empleado.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Desactivación de un empleado: siempre con fecha y motivo para la trazabilidad. */
public record DesactivacionRequest(
        @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
        @NotBlank(message = "El motivo es obligatorio") @Size(max = 200) String motivo) {
}
