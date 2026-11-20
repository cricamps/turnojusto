package cl.turnojusto.empleados.seguridad;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/** Acceso al {@link ContextoUsuario} de la solicitud HTTP en curso. */
public final class ContextoActual {

    static final String ATRIBUTO = ContextoActual.class.getName();

    private ContextoActual() {
    }

    /** Devuelve el contexto resuelto por {@link FiltroContexto}; falla si no hay usuario autenticado. */
    public static ContextoUsuario obtener() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        Object contexto = atributos == null ? null
                : atributos.getAttribute(ATRIBUTO, RequestAttributes.SCOPE_REQUEST);
        if (contexto instanceof ContextoUsuario c) {
            return c;
        }
        throw new IllegalStateException("No hay un usuario autenticado en la solicitud");
    }
}
