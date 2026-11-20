package cl.turnojusto.empleados.integracion;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Crea las cuentas en Microsoft Entra mediante Microsoft Graph cuando el Administrador
 * registra a un empleado. Si ya existe un usuario con ese correo lo reutiliza; si no, envía
 * una invitación (B2B) para que la persona inicie sesión con su propio correo.
 *
 * <p>Usa el flujo client credentials de la aplicación TurnoJusto con los permisos de
 * aplicación User.Invite.All y User.Read.All (consentidos por el administrador del inquilino).</p>
 */
@Component
public class EntraGraphClient implements ProveedorIdentidad {

    private static final Logger LOG = LoggerFactory.getLogger(EntraGraphClient.class);
    private static final String GRAPH = "https://graph.microsoft.com/v1.0";

    private final RestClient cliente;
    private final String inquilino;
    private final String clienteId;
    private final String secreto;
    private final String urlAplicacion;
    private String token;
    private Instant venceToken = Instant.EPOCH;

    public EntraGraphClient(RestClient.Builder builder,
                            @Value("${tj.entra.inquilino:}") String inquilino,
                            @Value("${tj.entra.cliente-id:}") String clienteId,
                            @Value("${tj.entra.secreto:}") String secreto,
                            @Value("${tj.entra.url-aplicacion:}") String urlAplicacion) {
        this.cliente = builder.build();
        this.inquilino = inquilino;
        this.clienteId = clienteId;
        this.secreto = secreto;
        this.urlAplicacion = urlAplicacion;
    }

    private boolean configurado() {
        return !inquilino.isBlank() && !clienteId.isBlank() && !secreto.isBlank();
    }

    @Override
    public Optional<String> crearCuenta(String correo, String nombre) {
        if (!configurado()) {
            LOG.warn("Microsoft Graph no está configurado: la cuenta de {} se vinculará al primer ingreso", correo);
            return Optional.empty();
        }
        try {
            Optional<String> existente = buscarPorCorreo(correo);
            if (existente.isPresent()) {
                return existente;
            }
            Map<?, ?> r = cliente.post().uri(GRAPH + "/invitations")
                    .headers(h -> h.setBearerAuth(tokenAplicacion()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "invitedUserEmailAddress", correo,
                            "invitedUserDisplayName", nombre,
                            "inviteRedirectUrl", urlAplicacion.isBlank() ? "https://myapps.microsoft.com" : urlAplicacion,
                            "sendInvitationMessage", true,
                            "invitedUserMessageInfo", Map.of("customizedMessageBody",
                                    "Tu empresa te registró en TurnoJusto. Acepta la invitación para ver tus turnos.")))
                    .retrieve().body(Map.class);
            Object usuario = r == null ? null : r.get("invitedUser");
            String oid = usuario instanceof Map<?, ?> u ? (String) u.get("id") : null;
            LOG.info("Invitación de Entra enviada a {} (oid {})", correo, oid);
            return Optional.ofNullable(oid);
        } catch (RestClientException e) {
            LOG.warn("No se pudo crear la cuenta de Entra para {}: {}", correo, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> buscarPorCorreo(String correo) {
        String filtro = "mail eq '" + correo.replace("'", "''") + "' or otherMails/any(m:m eq '"
                + correo.replace("'", "''") + "')";
        Map<?, ?> r = cliente.get().uri(GRAPH + "/users?$select=id,mail&$filter={f}", filtro)
                .headers(h -> {
                    h.setBearerAuth(tokenAplicacion());
                    h.set("ConsistencyLevel", "eventual");
                })
                .retrieve().body(Map.class);
        if (r != null && r.get("value") instanceof List<?> lista && !lista.isEmpty()
                && lista.getFirst() instanceof Map<?, ?> u) {
            return Optional.ofNullable((String) u.get("id"));
        }
        return Optional.empty();
    }

    private synchronized String tokenAplicacion() {
        if (token != null && Instant.now().isBefore(venceToken)) {
            return token;
        }
        var form = new LinkedMultiValueMap<String, String>();
        form.add("client_id", clienteId);
        form.add("client_secret", secreto);
        form.add("scope", "https://graph.microsoft.com/.default");
        form.add("grant_type", "client_credentials");
        Map<?, ?> r = cliente.post().uri("https://login.microsoftonline.com/{t}/oauth2/v2.0/token", inquilino)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(Map.class);
        token = (String) r.get("access_token");
        venceToken = Instant.now().plusSeconds(((Number) r.get("expires_in")).longValue() - 120);
        return token;
    }
}
