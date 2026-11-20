package cl.turnojusto.organizacion.empresa;

import cl.turnojusto.organizacion.comun.Auditoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Empresa cliente de la plataforma (tenant), tabla ORG.EMPRESA. */
@Entity
@Table(name = "EMPRESA")
public class Empresa extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EMPRESA_ID")
    private Long id;

    @Column(name = "RUT", nullable = false, length = 12)
    private String rut;

    @Column(name = "RAZON_SOCIAL", nullable = false, length = 150)
    private String razonSocial;

    @Column(name = "NOMBRE_FANTASIA", length = 100)
    private String nombreFantasia;

    @Column(name = "CORREO_CONTACTO", nullable = false, length = 254)
    private String correoContacto;

    @Column(name = "TELEFONO", length = 20)
    private String telefono;

    @Column(name = "ESTADO", nullable = false, length = 10)
    private String estado = "ACTIVA";

    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;

    protected Empresa() {
        // Requerido por JPA
    }

    public Empresa(String rut, String razonSocial, String correoContacto) {
        this.rut = rut;
        this.razonSocial = razonSocial;
        this.correoContacto = correoContacto;
    }

    /** El RUT de la empresa no se edita: identifica al cliente. */
    public void actualizar(String razonSocial, String nombreFantasia, String correoContacto, String telefono) {
        this.razonSocial = razonSocial;
        this.nombreFantasia = nombreFantasia;
        this.correoContacto = correoContacto;
        this.telefono = telefono;
    }

    public Long getId() {
        return id;
    }

    public String getRut() {
        return rut;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public String getNombreFantasia() {
        return nombreFantasia;
    }

    public String getCorreoContacto() {
        return correoContacto;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEstado() {
        return estado;
    }

    public Long getVersion() {
        return version;
    }
}
