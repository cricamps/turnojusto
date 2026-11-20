package cl.turnojusto.organizacion.region;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Catálogo de las 16 regiones de Chile (ORG.REGION). Define los feriados regionales. */
@Entity
@Table(name = "REGION")
public class Region {

    @Id
    @Column(name = "REGION_CODIGO", length = 5)
    private String codigo;

    @Column(name = "NOMBRE", nullable = false, length = 60)
    private String nombre;

    @Column(name = "ORDEN", nullable = false)
    private int orden;

    protected Region() {
        // Requerido por JPA
    }

    public Region(String codigo, String nombre, int orden) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.orden = orden;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public int getOrden() {
        return orden;
    }
}
