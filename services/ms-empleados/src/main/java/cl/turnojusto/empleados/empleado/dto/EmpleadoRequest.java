package cl.turnojusto.empleados.empleado.dto;

import cl.turnojusto.empleados.comun.RutValido;
import cl.turnojusto.empleados.seguridad.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Datos para registrar o editar una ficha (pantalla P03 · Formulario). */
public record EmpleadoRequest(
        @NotBlank(message = "El RUT es obligatorio") @RutValido String rut,
        @NotBlank(message = "Los nombres son obligatorios") @Size(max = 80) String nombres,
        @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 80) String apellidos,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido") @Size(max = 254)
        String correo,
        @Pattern(regexp = "^$|^\\+?[0-9 ]{8,15}$", message = "El teléfono no es válido") String telefono,
        @Size(max = 80) String cargo,
        @NotNull(message = "El rol es obligatorio") Rol rol,
        @NotNull(message = "La sucursal es obligatoria") Long sucursalId,
        Long equipoId,
        @NotNull(message = "La fecha de ingreso a la rotación es obligatoria")
        @PastOrPresent(message = "La fecha de ingreso no puede ser futura") LocalDate fechaIngresoRotacion) {
}
