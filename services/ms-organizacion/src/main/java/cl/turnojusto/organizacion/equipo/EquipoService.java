package cl.turnojusto.organizacion.equipo;

import cl.turnojusto.organizacion.comun.ConflictoException;
import cl.turnojusto.organizacion.comun.RecursoNoEncontradoException;
import cl.turnojusto.organizacion.comun.ReglaNegocioException;
import cl.turnojusto.organizacion.equipo.EquipoDtos.CoordinacionResponse;
import cl.turnojusto.organizacion.equipo.EquipoDtos.EquipoRequest;
import cl.turnojusto.organizacion.equipo.EquipoDtos.EquipoResponse;
import cl.turnojusto.organizacion.integracion.ConsultaEmpleados;
import cl.turnojusto.organizacion.integracion.ConsultaEmpleados.EmpleadoResumen;
import cl.turnojusto.organizacion.seguridad.ContextoUsuario;
import cl.turnojusto.organizacion.sucursal.Sucursal;
import cl.turnojusto.organizacion.sucursal.SucursalRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de equipos y coordinadores (F4). Reglas: un equipo no se repite en la misma
 * sucursal (CP-18), solo se crea en sucursales activas y su coordinador debe ser un
 * empleado activo de esa sucursal con rol Coordinador o Administrador (CP-19).
 */
@Service
public class EquipoService {

    private final EquipoRepository equipos;
    private final CoordinadorEquipoRepository coordinaciones;
    private final SucursalRepository sucursales;
    private final ConsultaEmpleados empleados;
    private final Clock reloj;

    public EquipoService(EquipoRepository equipos, CoordinadorEquipoRepository coordinaciones,
                         SucursalRepository sucursales, ConsultaEmpleados empleados, Optional<Clock> reloj) {
        this.equipos = equipos;
        this.coordinaciones = coordinaciones;
        this.sucursales = sucursales;
        this.empleados = empleados;
        this.reloj = reloj.orElse(Clock.systemDefaultZone());
    }

    @Transactional(readOnly = true)
    public List<EquipoResponse> listar(ContextoUsuario ctx, Long sucursalId) {
        Long filtro = ctx.esAdministrador() ? sucursalId : ctx.sucursalId();
        List<Equipo> lista = filtro == null
                ? equipos.findByEmpresaIdOrderByNombreAsc(ctx.empresaId())
                : equipos.findByEmpresaIdAndSucursalIdOrderByNombreAsc(ctx.empresaId(), filtro);
        Map<Long, CoordinadorEquipo> vigentes = coordinaciones.findByEmpresaIdAndVigenteHastaIsNull(ctx.empresaId())
                .stream().collect(Collectors.toMap(CoordinadorEquipo::getEquipoId, Function.identity(), (a, b) -> a));
        return lista.stream().map(e -> aRespuesta(e, vigentes.get(e.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public EquipoResponse obtener(ContextoUsuario ctx, Long id) {
        Equipo e = cargar(ctx, id);
        if (!ctx.esAdministrador() && !Objects.equals(e.getSucursalId(), ctx.sucursalId())) {
            throw new RecursoNoEncontradoException("El equipo no existe");
        }
        return aRespuesta(e, coordinaciones.findByEquipoIdAndVigenteHastaIsNull(id).orElse(null));
    }

    @Transactional
    public EquipoResponse crear(ContextoUsuario ctx, EquipoRequest req) {
        Sucursal s = sucursales.findByIdAndEmpresaId(req.sucursalId(), ctx.empresaId())
                .orElseThrow(() -> new ReglaNegocioException("La sucursal no existe"));
        if (!s.estaActiva()) {
            throw new ReglaNegocioException("No se pueden crear equipos en una sucursal inactiva");
        }
        String nombre = req.nombre().trim();
        if (equipos.existsBySucursalIdAndNombreIgnoreCase(s.getId(), nombre)) {
            throw new ConflictoException("Ya existe el equipo " + nombre + " en la sucursal " + s.getNombre());
        }
        Equipo e = new Equipo(ctx.empresaId(), s.getId(), nombre, blanco(req.descripcion()));
        e.auditar(ctx.oid());
        return aRespuesta(equipos.save(e), null);
    }

    @Transactional
    public EquipoResponse actualizar(ContextoUsuario ctx, Long id, EquipoRequest req) {
        Equipo e = cargar(ctx, id);
        if (!Objects.equals(e.getSucursalId(), req.sucursalId())) {
            throw new ReglaNegocioException("Un equipo no se puede mover de sucursal; crea uno nuevo");
        }
        String nombre = req.nombre().trim();
        if (equipos.existsBySucursalIdAndNombreIgnoreCaseAndIdNot(e.getSucursalId(), nombre, id)) {
            throw new ConflictoException("Ya existe el equipo " + nombre + " en esta sucursal");
        }
        e.actualizar(nombre, blanco(req.descripcion()));
        e.auditar(ctx.oid());
        return aRespuesta(equipos.save(e), coordinaciones.findByEquipoIdAndVigenteHastaIsNull(id).orElse(null));
    }

    @Transactional
    public EquipoResponse cambiarEstado(ContextoUsuario ctx, Long id, boolean activo) {
        Equipo e = cargar(ctx, id);
        e.cambiarEstado(activo);
        e.auditar(ctx.oid());
        return aRespuesta(equipos.save(e), coordinaciones.findByEquipoIdAndVigenteHastaIsNull(id).orElse(null));
    }

    @Transactional
    public EquipoResponse asignarCoordinador(ContextoUsuario ctx, Long id, Long empleadoId) {
        Equipo e = cargar(ctx, id);
        EmpleadoResumen emp = empleados.buscar(empleadoId)
                .orElseThrow(() -> new ReglaNegocioException("El empleado no existe"));
        if (!"ACTIVO".equals(emp.estado())) {
            throw new ReglaNegocioException("El empleado está inactivo");
        }
        if (!Objects.equals(emp.sucursalId(), e.getSucursalId())) {
            throw new ReglaNegocioException("El coordinador debe pertenecer a la sucursal del equipo");
        }
        if ("EMPLEADO".equals(emp.rol())) {
            throw new ReglaNegocioException("El empleado debe tener rol Coordinador o Administrador");
        }
        LocalDate hoy = LocalDate.now(reloj);
        Optional<CoordinadorEquipo> actual = coordinaciones.findByEquipoIdAndVigenteHastaIsNull(id);
        if (actual.isPresent() && Objects.equals(actual.get().getEmpleadoId(), empleadoId)) {
            return aRespuesta(e, actual.get());
        }
        actual.ifPresent(c -> {
            c.cerrar(hoy);
            c.auditar(ctx.oid());
            coordinaciones.saveAndFlush(c);
        });
        CoordinadorEquipo nueva = new CoordinadorEquipo(ctx.empresaId(), id, empleadoId, hoy);
        nueva.auditar(ctx.oid());
        return aRespuesta(e, coordinaciones.save(nueva));
    }

    @Transactional(readOnly = true)
    public List<CoordinacionResponse> historialCoordinadores(ContextoUsuario ctx, Long id) {
        obtener(ctx, id);
        return coordinaciones.findByEquipoIdOrderByVigenteDesdeDesc(id).stream()
                .map(c -> new CoordinacionResponse(c.getEmpleadoId(), c.getVigenteDesde(), c.getVigenteHasta()))
                .toList();
    }

    private Equipo cargar(ContextoUsuario ctx, Long id) {
        return equipos.findByIdAndEmpresaId(id, ctx.empresaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El equipo no existe"));
    }

    private static EquipoResponse aRespuesta(Equipo e, CoordinadorEquipo c) {
        return new EquipoResponse(e.getId(), e.getSucursalId(), e.getNombre(), e.getDescripcion(), e.getEstado(),
                c == null ? null : c.getEmpleadoId(), c == null ? null : c.getVigenteDesde(), e.getVersion());
    }

    private static String blanco(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
