package cl.turnojusto.organizacion.equipo;

import cl.turnojusto.organizacion.comun.Auditoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Coordinación de un equipo por un empleado (ORG.COORDINADOR_EQUIPO). Se conserva el
 * historial: al cambiar de coordinador se cierra la vigencia del anterior.
 */
@Entity
@Table(name = "COORDINADOR_EQUIPO")
public class CoordinadorEquipo extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COORDINADOR_EQUIPO_ID")
    private Long id;

    @Column(name = "EMPRESA_ID", nullable = false)
    private Long empresaId;

    @Column(name = "EQUIPO_ID", nullable = false)
    private Long equipoId;

    @Column(name = "EMPLEADO_ID", nullable = false)
    private Long empleadoId;

    @Column(name = "VIGENTE_DESDE", nullable = false)
    private LocalDate vigenteDesde;

    @Column(name = "VIGENTE_HASTA")
    private LocalDate vigenteHasta;

    protected CoordinadorEquipo() {
        // Requerido por JPA
    }

    public CoordinadorEquipo(Long empresaId, Long equipoId, Long empleadoId, LocalDate desde) {
        this.empresaId = empresaId;
        this.equipoId = equipoId;
        this.empleadoId = empleadoId;
        this.vigenteDesde = desde;
    }

    public void cerrar(LocalDate hasta) {
        this.vigenteHasta = hasta.isBefore(vigenteDesde) ? vigenteDesde : hasta;
    }

    public Long getEmpleadoId() {
        return empleadoId;
    }

    public Long getEquipoId() {
        return equipoId;
    }

    public LocalDate getVigenteDesde() {
        return vigenteDesde;
    }

    public LocalDate getVigenteHasta() {
        return vigenteHasta;
    }
}
