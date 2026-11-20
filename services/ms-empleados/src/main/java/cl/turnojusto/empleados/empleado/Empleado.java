package cl.turnojusto.empleados.empleado;

import cl.turnojusto.empleados.comun.ReglaNegocioException;
import cl.turnojusto.empleados.seguridad.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.Objects;

/** Ficha del trabajador (tabla EMP.EMPLEADO). */
@Entity
@Table(name = "EMPLEADO")
public class Empleado extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EMPLEADO_ID")
    private Long id;

    @Column(name = "EMPRESA_ID", nullable = false, updatable = false)
    private Long empresaId;

    @Column(name = "SUCURSAL_ID", nullable = false)
    private Long sucursalId;

    @Column(name = "EQUIPO_ID")
    private Long equipoId;

    @Column(name = "USUARIO_OID", length = 36)
    private String usuarioOid;

    @Column(name = "RUT", nullable = false, length = 12)
    private String rut;

    @Column(name = "NOMBRES", nullable = false, length = 80)
    private String nombres;

    @Column(name = "APELLIDOS", nullable = false, length = 80)
    private String apellidos;

    @Column(name = "CORREO", nullable = false, length = 254)
    private String correo;

    @Column(name = "TELEFONO", length = 20)
    private String telefono;

    @Column(name = "CARGO", length = 80)
    private String cargo;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROL", nullable = false, length = 15)
    private Rol rol = Rol.EMPLEADO;

    @Column(name = "FECHA_INGRESO_ROTACION", nullable = false)
    private LocalDate fechaIngresoRotacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 10)
    private EstadoEmpleado estado = EstadoEmpleado.ACTIVO;

    @Column(name = "FECHA_DESACTIVACION")
    private LocalDate fechaDesactivacion;

    @Column(name = "MOTIVO_DESACTIVACION", length = 200)
    private String motivoDesactivacion;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;

    protected Empleado() {
        // Requerido por JPA
    }

    public Empleado(Long empresaId, Long sucursalId, Long equipoId, String rut, String nombres, String apellidos,
                    String correo, LocalDate fechaIngresoRotacion, Rol rol) {
        this.empresaId = Objects.requireNonNull(empresaId);
        this.sucursalId = Objects.requireNonNull(sucursalId);
        this.equipoId = equipoId;
        this.rut = rut;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.correo = correo;
        this.fechaIngresoRotacion = fechaIngresoRotacion;
        this.rol = rol == null ? Rol.EMPLEADO : rol;
    }

    /** Actualiza los datos personales y laborales (no cambia sucursal: eso es un traslado). */
    public void actualizarDatos(String rut, String nombres, String apellidos, String correo, String telefono,
                                String cargo, Rol rol, LocalDate fechaIngresoRotacion) {
        this.rut = rut;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.correo = correo;
        this.telefono = telefono;
        this.cargo = cargo;
        this.rol = rol;
        this.fechaIngresoRotacion = fechaIngresoRotacion;
    }

    /** Cambia sucursal y/o equipo. Debe registrarse además un {@link Traslado}. */
    public void trasladar(Long sucursalDestinoId, Long equipoDestinoId) {
        if (!estaActivo()) {
            throw new ReglaNegocioException("No se puede trasladar a un empleado inactivo");
        }
        if (Objects.equals(sucursalId, sucursalDestinoId) && Objects.equals(equipoId, equipoDestinoId)) {
            throw new ReglaNegocioException("El destino es igual a la ubicación actual del empleado");
        }
        this.sucursalId = sucursalDestinoId;
        this.equipoId = equipoDestinoId;
    }

    /** Saca al empleado de la rotación sin borrar su historial. */
    public void desactivar(LocalDate fecha, String motivo) {
        if (!estaActivo()) {
            throw new ReglaNegocioException("El empleado ya está inactivo");
        }
        if (fecha.isBefore(fechaIngresoRotacion)) {
            throw new ReglaNegocioException("La fecha de desactivación no puede ser anterior al ingreso a la rotación");
        }
        this.estado = EstadoEmpleado.INACTIVO;
        this.fechaDesactivacion = fecha;
        this.motivoDesactivacion = motivo;
    }

    /** Vuelve a incluir al empleado en la rotación. */
    public void reactivar() {
        if (estaActivo()) {
            throw new ReglaNegocioException("El empleado ya está activo");
        }
        this.estado = EstadoEmpleado.ACTIVO;
        this.fechaDesactivacion = null;
        this.motivoDesactivacion = null;
    }

    /** Asocia la cuenta de Entra External ID al iniciar sesión por primera vez. */
    public void vincularCuenta(String oid) {
        this.usuarioOid = oid;
    }

    public boolean estaActivo() {
        return estado == EstadoEmpleado.ACTIVO;
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

    public Long getEquipoId() {
        return equipoId;
    }

    public String getUsuarioOid() {
        return usuarioOid;
    }

    public String getRut() {
        return rut;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getCorreo() {
        return correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getCargo() {
        return cargo;
    }

    public Rol getRol() {
        return rol;
    }

    public LocalDate getFechaIngresoRotacion() {
        return fechaIngresoRotacion;
    }

    public EstadoEmpleado getEstado() {
        return estado;
    }

    public LocalDate getFechaDesactivacion() {
        return fechaDesactivacion;
    }

    public String getMotivoDesactivacion() {
        return motivoDesactivacion;
    }

    public Long getVersion() {
        return version;
    }

    void setIdParaPruebas(Long id) {
        this.id = id;
    }
}
