package cl.turnojusto.empleados.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publica los eventos pendientes del outbox cada 10 segundos.
 *
 * <p>Sprint 1: los eventos quedan registrados en el log del contenedor (CloudWatch). En el
 * Sprint 2 este componente envía cada evento al tópico {@code eventos-turnos} de Azure
 * Service Bus, que consumen ms-reportes y fn-notificaciones.</p>
 */
@Component
@ConditionalOnProperty(name = "tj.outbox.publicar", havingValue = "true", matchIfMissing = true)
public class PublicadorOutbox {

    private static final Logger LOG = LoggerFactory.getLogger(PublicadorOutbox.class);

    private final OutboxRepository repositorio;

    public PublicadorOutbox(OutboxRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Scheduled(fixedDelayString = "${tj.outbox.intervalo-ms:10000}")
    @Transactional
    public void publicarPendientes() {
        for (OutboxEvento evento : repositorio.findTop20ByEstadoOrderByCreadoEnAsc("PENDIENTE")) {
            try {
                LOG.info("Evento {} publicado: {} {}", evento.getTipoEvento(), evento.getId(), evento.getCarga());
                evento.marcarPublicado();
            } catch (RuntimeException e) {
                LOG.warn("No se pudo publicar el evento {}", evento.getId(), e);
                evento.registrarFallo();
            }
        }
    }
}
