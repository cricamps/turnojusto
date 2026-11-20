package cl.turnojusto.organizacion.equipo;

import cl.turnojusto.organizacion.comun.Auditoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Equipo o área dentro de una sucursal (ORG.EQUIPO). */
@Entity
@Table(name = "EQUIPO")
public class Equipo extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EQUIPO_ID")
    private Long id;

    @Column(name = "EMPRESA_ID", nullable = false, updatable = false)
    private Long empresaId;

    @Column(name = "SUCURSAL_ID", nullable = false, updatable = false)
    private Long sucursalId;

    @Column(name = "NOMBRE", nullable = false, length = 80)
    private String nombre;

    @Column(name = "DESCRIPCION", length = 300)
    private String descripcion;

    @Column(name = "ESTADO", nullable = false, length = 10)
    private String estado = "ACTIVO";

    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;

    protected Equipo() {
        // Requerido por JPA
    }

    public Equipo(Long empresaId, Long sucursalId, String nombre, String descripcion) {
        this.empresaId = empresaId;
        this.sucursalId = sucursalId;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public void actualizar(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public void cambiarEstado(boolean activo) {
        this.estado = activo ? "ACTIVO" : "INACTIVO";
    }

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public Long getSucursalId() {
        return sucursalId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
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
