package cl.turnojusto.empleados.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Se ejecuta después de validar el token. Resuelve empresa, sucursal y rol del usuario,
 * los deja disponibles en {@link ContextoActual} y reemplaza las autoridades por
 * {@code ROLE_<ROL>} para que funcionen las anotaciones {@code @PreAuthorize}.
 * Si la cuenta no tiene ficha activa, responde 403.
 */
public class FiltroContexto extends OncePerRequestFilter {

    private final ResolutorContexto resolutor;

    public FiltroContexto(ResolutorContexto resolutor) {
        this.resolutor = resolutor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token) {
            Optional<ContextoUsuario> contexto = resolutor.resolver(token.getToken());
            if (contexto.isEmpty()) {
                responderSinAcceso(response);
                return;
            }
            ContextoUsuario c = contexto.get();
            request.setAttribute(ContextoActual.ATRIBUTO, c);
            var conRol = new JwtAuthenticationToken(token.getToken(),
                    List.of(new SimpleGrantedAuthority("ROLE_" + c.rol().name())), c.oid());
            SecurityContextHolder.getContext().setAuthentication(conRol);
        }
        chain.doFilter(request, response);
    }

    private static void responderSinAcceso(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"type":"https://turnojusto.cl/errores/sin-ficha","title":"Sin acceso","status":403,\
                "detail":"Tu cuenta no está asociada a una ficha activa de empleado. Pide al administrador de tu empresa que te registre con este correo."}""");
    }
}
