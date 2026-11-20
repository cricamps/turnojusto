package cl.turnojusto.empleados.empleado;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** Registro histórico de un cambio de sucursal o equipo (tabla EMP.TRASLADO). */
@Entity
@Table(name = "TRASLADO")
public class Traslado extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRASLADO_ID")
    private Long id;

    @Column(name = "EMPRESA_ID", nullable = false)
    private Long empresaId;

    @Column(name = "EMPLEADO_ID", nullable = false)
    private Long empleadoId;

    @Column(name = "SUCURSAL_ORIGEN_ID", nullable = false)
    private Long sucursalOrigenId;

    @Column(name = "SUCURSAL_DESTINO_ID", nullable = false)
    private Long sucursalDestinoId;

    @Column(name = "EQUIPO_ORIGEN_ID")
    private Long equipoOrigenId;

    @Column(name = "EQUIPO_DESTINO_ID")
    private Long equipoDestinoId;

    @Column(name = "FECHA_EFECTIVA", nullable = false)
    private LocalDate fechaEfectiva;

    @Column(name = "MOTIVO", length = 200)
    private String motivo;

    protected Traslado() {
        // Requerido por JPA
    }

    public Traslado(Empleado empleado, Long sucursalDestinoId, Long equipoDestinoId, LocalDate fechaEfectiva,
                    String motivo) {
        this.empresaId = empleado.getEmpresaId();
        this.empleadoId = empleado.getId();
        this.sucursalOrigenId = empleado.getSucursalId();
        this.equipoOrigenId = empleado.getEquipoId();
        this.sucursalDestinoId = sucursalDestinoId;
        this.equipoDestinoId = equipoDestinoId;
        this.fechaEfectiva = fechaEfectiva;
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public Long getEmpleadoId() {
        return empleadoId;
    }

    public Long getSucursalOrigenId() {
        return sucursalOrigenId;
    }

    public Long getSucursalDestinoId() {
        return sucursalDestinoId;
    }

    public Long getEquipoOrigenId() {
        return equipoOrigenId;
    }

    public Long getEquipoDestinoId() {
        return equipoDestinoId;
    }

    public LocalDate getFechaEfectiva() {
        return fechaEfectiva;
    }

    public String getMotivo() {
        return motivo;
    }
}
