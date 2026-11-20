package cl.turnojusto.organizacion.sucursal;

import cl.turnojusto.organizacion.comun.ConflictoException;
import cl.turnojusto.organizacion.comun.RecursoNoEncontradoException;
import cl.turnojusto.organizacion.comun.ReglaNegocioException;
import cl.turnojusto.organizacion.equipo.EquipoRepository;
import cl.turnojusto.organizacion.region.RegionRepository;
import cl.turnojusto.organizacion.seguridad.ContextoUsuario;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de sucursales (F2). El Administrador ve y administra todas las sucursales de
 * su empresa; Coordinador y Empleado solo ven la suya.
 */
@Service
public class SucursalService {

    private final SucursalRepository sucursales;
    private final EquipoRepository equipos;
    private final RegionRepository regiones;

    public SucursalService(SucursalRepository sucursales, EquipoRepository equipos, RegionRepository regiones) {
        this.sucursales = sucursales;
        this.equipos = equipos;
        this.regiones = regiones;
    }

    @Transactional(readOnly = true)
    public List<SucursalResponse> listar(ContextoUsuario ctx) {
        return sucursales.findByEmpresaIdOrderByNombreAsc(ctx.empresaId()).stream()
                .filter(s -> ctx.esAdministrador() || Objects.equals(s.getId(), ctx.sucursalId()))
                .map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public SucursalResponse obtener(ContextoUsuario ctx, Long id) {
        Sucursal s = cargar(ctx, id);
        if (!ctx.esAdministrador() && !Objects.equals(s.getId(), ctx.sucursalId())) {
            throw new RecursoNoEncontradoException("La sucursal no existe");
        }
        return aRespuesta(s);
    }

    @Transactional
    public SucursalResponse crear(ContextoUsuario ctx, SucursalRequest req) {
        validarUnicos(ctx, req, null);
        Sucursal s = new Sucursal(ctx.empresaId());
        s.definir(req.codigo(), req.nombre(), req.direccion(), req.comuna(), region(req), req.horaApertura(),
                req.horaCierre());
        s.auditar(ctx.oid());
        return aRespuesta(sucursales.save(s));
    }

    @Transactional
    public SucursalResponse actualizar(ContextoUsuario ctx, Long id, SucursalRequest req) {
        Sucursal s = cargar(ctx, id);
        validarUnicos(ctx, req, id);
        s.definir(req.codigo(), req.nombre(), req.direccion(), req.comuna(), region(req), req.horaApertura(),
                req.horaCierre());
        s.auditar(ctx.oid());
        return aRespuesta(sucursales.save(s));
    }

    @Transactional
    public SucursalResponse cambiarEstado(ContextoUsuario ctx, Long id, boolean activa) {
        Sucursal s = cargar(ctx, id);
        if (!activa && equipos.countBySucursalIdAndEstado(id, "ACTIVO") > 0) {
            throw new ReglaNegocioException("Primero desactiva los equipos activos de la sucursal");
        }
        s.cambiarEstado(activa);
        s.auditar(ctx.oid());
        return aRespuesta(sucursales.save(s));
    }

    private String region(SucursalRequest req) {
        if (!regiones.existsById(req.regionCodigo())) {
            throw new ReglaNegocioException("La región no existe");
        }
        return req.regionCodigo();
    }

    private void validarUnicos(ContextoUsuario ctx, SucursalRequest req, Long id) {
        boolean codigoRepetido = id == null
                ? sucursales.existsByEmpresaIdAndCodigoIgnoreCase(ctx.empresaId(), req.codigo().trim())
                : sucursales.existsByEmpresaIdAndCodigoIgnoreCaseAndIdNot(ctx.empresaId(), req.codigo().trim(), id);
        if (codigoRepetido) {
            throw new ConflictoException("Ya existe una sucursal con el código " + req.codigo().toUpperCase());
        }
        boolean nombreRepetido = id == null
                ? sucursales.existsByEmpresaIdAndNombreIgnoreCase(ctx.empresaId(), req.nombre().trim())
                : sucursales.existsByEmpresaIdAndNombreIgnoreCaseAndIdNot(ctx.empresaId(), req.nombre().trim(), id);
        if (nombreRepetido) {
            throw new ConflictoException("Ya existe una sucursal llamada " + req.nombre().trim());
        }
    }

    private Sucursal cargar(ContextoUsuario ctx, Long id) {
        return sucursales.findByIdAndEmpresaId(id, ctx.empresaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La sucursal no existe"));
    }

    private SucursalResponse aRespuesta(Sucursal s) {
        return SucursalResponse.de(s, equipos.countBySucursalIdAndEstado(s.getId(), "ACTIVO"));
    }
}
