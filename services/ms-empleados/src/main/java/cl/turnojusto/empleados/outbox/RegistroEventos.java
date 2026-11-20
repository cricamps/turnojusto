package cl.turnojusto.empleados.outbox;

import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Guarda eventos de dominio en el outbox dentro de la transacción del caso de uso. */
@Component
public class RegistroEventos {

    private final OutboxRepository repositorio;
    private final JsonMapper json;

    public RegistroEventos(OutboxRepository repositorio, JsonMapper json) {
        this.repositorio = repositorio;
        this.json = json;
    }

    public void registrar(Long empresaId, String tipoEvento, Long agregadoId, Map<String, Object> datos) {
        repositorio.save(new OutboxEvento(empresaId, tipoEvento, agregadoId, json.writeValueAsString(datos)));
    }
}
