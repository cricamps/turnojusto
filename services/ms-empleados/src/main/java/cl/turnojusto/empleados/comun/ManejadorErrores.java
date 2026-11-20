package cl.turnojusto.empleados.comun;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones a respuestas RFC 9457 (application/problem+json) con mensajes en
 * español que el frontend muestra directamente al usuario.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ProblemDetail noEncontrado(RecursoNoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "No encontrado", e.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    ProblemDetail reglaNegocio(ReglaNegocioException e) {
        return problema(HttpStatus.UNPROCESSABLE_CONTENT, "Regla de negocio", e.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    ProblemDetail conflicto(ConflictoException e) {
        return problema(HttpStatus.CONFLICT, "Dato duplicado", e.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail concurrencia(ObjectOptimisticLockingFailureException e) {
        return problema(HttpStatus.CONFLICT, "Edición simultánea",
                "Otra persona modificó este registro. Recarga la página y vuelve a intentarlo.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridad(DataIntegrityViolationException e) {
        LOG.warn("Restricción de la base de datos: {}", e.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "Dato rechazado por la base de datos",
                "El dato ya existe o no cumple una regla del modelo (por ejemplo, RUT o correo repetido).");
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    ProblemDetail accesoDenegado(RuntimeException e) {
        return problema(HttpStatus.FORBIDDEN, "Acceso denegado", "Tu rol no permite realizar esta acción.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacion(MethodArgumentNotValidException e) {
        ProblemDetail p = problema(HttpStatus.BAD_REQUEST, "Datos inválidos", "Revisa los campos marcados.");
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        p.setProperty("campos", campos);
        return p;
    }

    private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(estado, detalle);
        p.setTitle(titulo);
        p.setType(URI.create("https://turnojusto.cl/errores/" + estado.value()));
        return p;
    }
}
