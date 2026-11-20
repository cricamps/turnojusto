package cl.turnojusto.organizacion.comun;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.OffsetDateTime;

/**
 * Columnas de auditoría comunes (CREADO_EN/POR, ACTUALIZADO_EN/POR). El "por" guarda el
 * Object ID de Entra External ID de quien hizo el cambio.
 */
@MappedSuperclass
public abstract class Auditoria {

    @Column(name = "CREADO_EN", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "CREADO_POR", nullable = false, updatable = false, length = 36)
    private String creadoPor;

    @Column(name = "ACTUALIZADO_EN")
    private OffsetDateTime actualizadoEn;

    @Column(name = "ACTUALIZADO_POR", length = 36)
    private String actualizadoPor;

    /** Registra quién hace el cambio; se llama desde el caso de uso antes de guardar. */
    public void auditar(String usuarioOid) {
        if (creadoPor == null) {
            creadoPor = usuarioOid;
        } else {
            actualizadoPor = usuarioOid;
        }
    }

    @PrePersist
    void alCrear() {
        creadoEn = OffsetDateTime.now();
        if (creadoPor == null) {
            creadoPor = "sistema";
        }
    }

    @PreUpdate
    void alActualizar() {
        actualizadoEn = OffsetDateTime.now();
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public OffsetDateTime getActualizadoEn() {
        return actualizadoEn;
    }
}
