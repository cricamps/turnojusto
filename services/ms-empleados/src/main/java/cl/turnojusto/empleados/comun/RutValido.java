package cl.turnojusto.empleados.comun;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Valida que un campo contenga un RUT chileno con dígito verificador correcto. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RutValido.Validador.class)
public @interface RutValido {

    String message() default "El RUT no es válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Implementación de la validación; los valores nulos los controla {@code @NotBlank}. */
    class Validador implements ConstraintValidator<RutValido, String> {
        @Override
        public boolean isValid(String valor, ConstraintValidatorContext contexto) {
            return valor == null || valor.isBlank() || Rut.esValido(valor);
        }
    }
}
