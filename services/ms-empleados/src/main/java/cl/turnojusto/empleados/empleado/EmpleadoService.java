package cl.turnojusto.empleados.empleado;

import cl.turnojusto.empleados.comun.ConflictoException;
import cl.turnojusto.empleados.comun.Pagina;
import cl.turnojusto.empleados.comun.RecursoNoEncontradoException;
import cl.turnojusto.empleados.comun.ReglaNegocioException;
import cl.turnojusto.empleados.comun.Rut;
import cl.turnojusto.empleados.empleado.dto.DesactivacionRequest;
import cl.turnojusto.empleados.empleado.dto.EmpleadoRequest;
import cl.turnojusto.empleados.empleado.dto.EmpleadoResponse;
import cl.turnojusto.empleados.empleado.dto.ResumenResponse;
import cl.turnojusto.empleados.empleado.dto.TrasladoRequest;
import cl.turnojusto.empleados.empleado.dto.TrasladoResponse;
import cl.turnojusto.empleados.integracion.ProveedorIdentidad;
import cl.turnojusto.empleados.integracion.ValidadorUbicacion;
import cl.turnojusto.empleados.outbox.RegistroEventos;
import cl.turnojusto.empleados.seguridad.ContextoUsuario;
import cl.turnojusto.empleados.seguridad.Rol;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de la gestión de empleados (funcionalidad F3).
 *
 * <p>Reglas de aislamiento: todo se filtra por la empresa del usuario; el Coordinador solo ve
 * su sucursal y el Empleado solo su propia ficha. Las escrituras son del Administrador.</p>
 */
@Service
public class EmpleadoService {

    private final EmpleadoRepository empleados;
    private final TrasladoRepository traslados;
    private final ValidadorUbicacion ubicacion;
    private final RegistroEventos eventos;
    private final ProveedorIdentidad identidad;

    public EmpleadoService(EmpleadoRepository empleados, TrasladoRepository traslados, ValidadorUbicacion ubicacion,
                           RegistroEventos eventos, ProveedorIdentidad identidad) {
        this.empleados = empleados;
        this.traslados = traslados;
        this.ubicacion = ubicacion;
        this.eventos = eventos;
        this.identidad = identidad;
    }

    @Transactional(readOnly = true)
    public Pagina<EmpleadoResponse> buscar(ContextoUsuario ctx, String texto, Long sucursalId, Long equipoId,
                                           EstadoEmpleado estado, int pagina, int tamano) {
        Long sucursalFiltro = sucursalId;
        if (ctx.rol() == Rol.COORDINADOR) {
            // El Coordinador solo puede ver su propia sucursal (caso CP-13).
            sucursalFiltro = ctx.sucursalId();
        } else if (ctx.rol() == Rol.EMPLEADO) {
            throw new AccessDeniedException("El rol Empleado no puede listar empleados");
        }
        List<Specification<Empleado>> filtros = Stream.of(EmpleadoSpecs.deEmpresa(ctx.empresaId()),
                        EmpleadoSpecs.deSucursal(sucursalFiltro), EmpleadoSpecs.deEquipo(equipoId),
                        EmpleadoSpecs.conEstado(estado), EmpleadoSpecs.texto(texto))
                .filter(Objects::nonNull).toList();
        var orden = Sort.by("apellidos", "nombres");
        var page = empleados.findAll(Specification.allOf(filtros),
                PageRequest.of(Math.max(pagina, 0), Math.clamp(tamano, 1, 100), orden));
        return Pagina.de(page, EmpleadoResponse::de);
    }

    @Transactional(readOnly = true)
    public EmpleadoResponse obtener(ContextoUsuario ctx, Long id) {
        return EmpleadoResponse.de(cargarVisible(ctx, id));
    }

    @Transactional
    public EmpleadoResponse crear(ContextoUsuario ctx, EmpleadoRequest req) {
        String rut = Rut.normalizar(req.rut());
        String correo = req.correo().trim().toLowerCase();
        if (empleados.existsByEmpresaIdAndRut(ctx.empresaId(), rut)) {
            throw new ConflictoException("Ya existe un empleado con el RUT " + rut + " en la empresa");
        }
        if (empleados.existsByEmpresaIdAndCorreoIgnoreCase(ctx.empresaId(), correo)) {
            throw new ConflictoException("Ya existe un empleado con el correo " + correo + " en la empresa");
        }
        ubicacion.validar(req.sucursalId(), req.equipoId());
        Empleado e = new Empleado(ctx.empresaId(), req.sucursalId(), req.equipoId(), rut, req.nombres().trim(),
                req.apellidos().trim(), correo, req.fechaIngresoRotacion(), req.rol());
        e.actualizarDatos(rut, req.nombres().trim(), req.apellidos().trim(), correo, vacioANull(req.telefono()),
                vacioANull(req.cargo()), req.rol(), req.fechaIngresoRotacion());
        e.auditar(ctx.oid());
        // La ficha queda en Oracle; la cuenta para iniciar sesión se crea en Entra (IDaaS).
        identidad.crearCuenta(correo, e.getNombres() + " " + e.getApellidos())
                .filter(oid -> empleados.findByUsuarioOid(oid).isEmpty())
                .ifPresent(e::vincularCuenta);
        Empleado guardado = empleados.save(e);
        eventos.registrar(ctx.empresaId(), "EmpleadoRegistrado", guardado.getId(), datosEvento(guardado));
        return EmpleadoResponse.de(guardado);
    }

    @Transactional
    public EmpleadoResponse actualizar(ContextoUsuario ctx, Long id, EmpleadoRequest req) {
        Empleado e = cargar(ctx, id);
        String rut = Rut.normalizar(req.rut());
        String correo = req.correo().trim().toLowerCase();
        if (empleados.existsByEmpresaIdAndRutAndIdNot(ctx.empresaId(), rut, id)) {
            throw new ConflictoException("Ya existe otro empleado con el RUT " + rut + " en la empresa");
        }
        if (empleados.existsByEmpresaIdAndCorreoIgnoreCaseAndIdNot(ctx.empresaId(), correo, id)) {
            throw new ConflictoException("Ya existe otro empleado con el correo " + correo + " en la empresa");
        }
        if (!Objects.equals(e.getSucursalId(), req.sucursalId()) || !Objects.equals(e.getEquipoId(), req.equipoId())) {
            throw new ReglaNegocioException("Para cambiar la sucursal o el equipo usa la opción Trasladar");
        }
        if (Objects.equals(e.getId(), ctx.empleadoId()) && req.rol() != ctx.rol()) {
            throw new ReglaNegocioException("No puedes cambiar tu propio rol");
        }
        e.actualizarDatos(rut, req.nombres().trim(), req.apellidos().trim(), correo, vacioANull(req.telefono()),
                vacioANull(req.cargo()), req.rol(), req.fechaIngresoRotacion());
        e.auditar(ctx.oid());
        return EmpleadoResponse.de(empleados.save(e));
    }

    @Transactional
    public EmpleadoResponse trasladar(ContextoUsuario ctx, Long id, TrasladoRequest req) {
        Empleado e = cargar(ctx, id);
        ubicacion.validar(req.sucursalDestinoId(), req.equipoDestinoId());
        Traslado t = new Traslado(e, req.sucursalDestinoId(), req.equipoDestinoId(), req.fechaEfectiva(),
                vacioANull(req.motivo()));
        t.auditar(ctx.oid());
        e.trasladar(req.sucursalDestinoId(), req.equipoDestinoId());
        e.auditar(ctx.oid());
        traslados.save(t);
        Empleado guardado = empleados.save(e);
        Map<String, Object> datos = datosEvento(guardado);
        datos.put("sucursalOrigenId", t.getSucursalOrigenId());
        datos.put("equipoOrigenId", t.getEquipoOrigenId());
        datos.put("fechaEfectiva", req.fechaEfectiva().toString());
        eventos.registrar(ctx.empresaId(), "EmpleadoTrasladado", guardado.getId(), datos);
        return EmpleadoResponse.de(guardado);
    }

    @Transactional
    public EmpleadoResponse desactivar(ContextoUsuario ctx, Long id, DesactivacionRequest req) {
        Empleado e = cargar(ctx, id);
        if (Objects.equals(e.getId(), ctx.empleadoId())) {
            throw new ReglaNegocioException("No puedes desactivar tu propia cuenta");
        }
        e.desactivar(req.fecha(), req.motivo().trim());
        e.auditar(ctx.oid());
        Empleado guardado = empleados.save(e);
        Map<String, Object> datos = datosEvento(guardado);
        datos.put("fechaDesactivacion", req.fecha().toString());
        eventos.registrar(ctx.empresaId(), "EmpleadoDesactivado", guardado.getId(), datos);
        return EmpleadoResponse.de(guardado);
    }

    @Transactional
    public EmpleadoResponse reactivar(ContextoUsuario ctx, Long id) {
        Empleado e = cargar(ctx, id);
        ubicacion.validar(e.getSucursalId(), e.getEquipoId());
        e.reactivar();
        e.auditar(ctx.oid());
        Empleado guardado = empleados.save(e);
        eventos.registrar(ctx.empresaId(), "EmpleadoReactivado", guardado.getId(), datosEvento(guardado));
        return EmpleadoResponse.de(guardado);
    }

    @Transactional(readOnly = true)
    public List<TrasladoResponse> historialTraslados(ContextoUsuario ctx, Long id) {
        cargarVisible(ctx, id);
        return traslados.findByEmpleadoIdOrderByFechaEfectivaDesc(id).stream().map(TrasladoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ResumenResponse resumen(ContextoUsuario ctx) {
        return new ResumenResponse(empleados.countByEmpresaIdAndEstado(ctx.empresaId(), EstadoEmpleado.ACTIVO),
                empleados.countByEmpresaIdAndEstado(ctx.empresaId(), EstadoEmpleado.INACTIVO));
    }

    /** Carga una ficha de la empresa del usuario; si es de otra empresa responde 404. */
    private Empleado cargar(ContextoUsuario ctx, Long id) {
        return empleados.findByIdAndEmpresaId(id, ctx.empresaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El empleado no existe"));
    }

    /** Además aplica la regla de visibilidad por rol. */
    private Empleado cargarVisible(ContextoUsuario ctx, Long id) {
        Empleado e = cargar(ctx, id);
        boolean visible = switch (ctx.rol()) {
            case ADMINISTRADOR -> true;
            case COORDINADOR -> Objects.equals(e.getSucursalId(), ctx.sucursalId());
            case EMPLEADO -> Objects.equals(e.getId(), ctx.empleadoId());
        };
        if (!visible) {
            throw new RecursoNoEncontradoException("El empleado no existe");
        }
        return e;
    }

    private static Map<String, Object> datosEvento(Empleado e) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("empleadoId", e.getId());
        datos.put("empresaId", e.getEmpresaId());
        datos.put("sucursalId", e.getSucursalId());
        datos.put("equipoId", e.getEquipoId());
        datos.put("estado", e.getEstado().name());
        return datos;
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
