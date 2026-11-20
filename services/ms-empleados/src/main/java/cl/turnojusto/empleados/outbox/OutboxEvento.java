package cl.turnojusto.empleados.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Evento pendiente de publicar (patrón outbox, tabla EMP.OUTBOX_EVENTO). Se guarda en la
 * misma transacción que el cambio de negocio, así un traslado nunca queda sin su evento.
 */
@Entity
@Table(name = "OUTBOX_EVENTO")
public class OutboxEvento {

    @Id
    @Column(name = "EVENTO_ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", nullable = false)
    private Long empresaId;

    @Column(name = "TIPO_EVENTO", nullable = false, length = 40)
    private String tipoEvento;

    @Column(name = "AGREGADO_ID", nullable = false)
    private Long agregadoId;

    @Lob
    @Column(name = "CARGA", nullable = false)
    private String carga;

    @Column(name = "ESTADO", nullable = false, length = 10)
    private String estado = "PENDIENTE";

    @Column(name = "INTENTOS", nullable = false)
    private int intentos;

    @Column(name = "CREADO_EN", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "PUBLICADO_EN")
    private OffsetDateTime publicadoEn;

    protected OutboxEvento() {
        // Requerido por JPA
    }

    public OutboxEvento(Long empresaId, String tipoEvento, Long agregadoId, String carga) {
        this.id = UUID.randomUUID().toString();
        this.empresaId = empresaId;
        this.tipoEvento = tipoEvento;
        this.agregadoId = agregadoId;
        this.carga = carga;
    }

    @PrePersist
    void alCrear() {
        creadoEn = OffsetDateTime.now();
    }

    public void marcarPublicado() {
        estado = "PUBLICADO";
        publicadoEn = OffsetDateTime.now();
    }

    public void registrarFallo() {
        intentos++;
        if (intentos >= 5) {
            estado = "ERROR";
        }
    }

    public String getId() {
        return id;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public String getCarga() {
        return carga;
    }

    public String getEstado() {
        return estado;
    }

    public Long getAgregadoId() {
        return agregadoId;
    }
}
