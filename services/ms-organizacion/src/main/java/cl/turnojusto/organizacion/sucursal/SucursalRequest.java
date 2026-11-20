package cl.turnojusto.organizacion.sucursal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos del formulario de sucursal. */
public record SucursalRequest(
        @NotBlank(message = "El código es obligatorio") @Size(max = 10)
        @Pattern(regexp = "^[A-Za-z0-9]{2,10}$", message = "Usa de 2 a 10 letras o números, sin espacios") String codigo,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) String nombre,
        @NotBlank(message = "La dirección es obligatoria") @Size(max = 150) String direccion,
        @NotBlank(message = "La comuna es obligatoria") @Size(max = 60) String comuna,
        @NotBlank(message = "La región es obligatoria") String regionCodigo,
        @NotBlank @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$", message = "Usa el formato HH:MM") String horaApertura,
        @NotBlank @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$", message = "Usa el formato HH:MM") String horaCierre) {
}
