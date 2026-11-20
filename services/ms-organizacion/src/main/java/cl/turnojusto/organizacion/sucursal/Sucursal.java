package cl.turnojusto.organizacion.sucursal;

import cl.turnojusto.organizacion.comun.Auditoria;
import cl.turnojusto.organizacion.comun.ReglaNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalTime;

/** Local o sede de una empresa (ORG.SUCURSAL). */
@Entity
@Table(name = "SUCURSAL")
public class Sucursal extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SUCURSAL_ID")
    private Long id;

    @Column(name = "EMPRESA_ID", nullable = false, updatable = false)
    private Long empresaId;

    @Column(name = "CODIGO", nullable = false, length = 10)
    private String codigo;

    @Column(name = "NOMBRE", nullable = false, length = 80)
    private String nombre;

    @Column(name = "DIRECCION", nullable = false, length = 150)
    private String direccion;

    @Column(name = "COMUNA", nullable = false, length = 60)
    private String comuna;

    @Column(name = "REGION_CODIGO", nullable = false, length = 5)
    private String regionCodigo;

    @Column(name = "HORA_APERTURA", nullable = false, length = 5)
    private String horaApertura;

    @Column(name = "HORA_CIERRE", nullable = false, length = 5)
    private String horaCierre;

    @Column(name = "ESTADO", nullable = false, length = 10)
    private String estado = "ACTIVA";

    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;

    protected Sucursal() {
        // Requerido por JPA
    }

    public Sucursal(Long empresaId) {
        this.empresaId = empresaId;
    }

    /**
     * Actualiza los datos de la sucursal. Valida el horario (CP-26: la hora de cierre debe ser
     * posterior a la de apertura) antes de que llegue a la restricción de la base.
     */
    public void definir(String codigo, String nombre, String direccion, String comuna, String regionCodigo,
                        String horaApertura, String horaCierre) {
        LocalTime apertura = LocalTime.parse(horaApertura);
        LocalTime cierre = LocalTime.parse(horaCierre);
        if (!cierre.isAfter(apertura)) {
            throw new ReglaNegocioException("La hora de cierre debe ser posterior a la hora de apertura");
        }
        this.codigo = codigo.trim().toUpperCase();
        this.nombre = nombre.trim();
        this.direccion = direccion.trim();
        this.comuna = comuna.trim();
        this.regionCodigo = regionCodigo;
        this.horaApertura = horaApertura;
        this.horaCierre = horaCierre;
    }

    public void cambiarEstado(boolean activa) {
        this.estado = activa ? "ACTIVA" : "INACTIVA";
    }

    public boolean estaActiva() {
        return "ACTIVA".equals(estado);
    }

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getComuna() {
        return comuna;
    }

    public String getRegionCodigo() {
        return regionCodigo;
    }

    public String getHoraApertura() {
        return horaApertura;
    }

    public String getHoraCierre() {
        return horaCierre;
    }

    public String getEstado() {
        return estado;
    }

    public Long getVersion() {
        return version;
    }

    void setIdParaPruebas(Long id) {
        this.id = id;
    }
}
