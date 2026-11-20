package cl.turnojusto.empleados.empleado;

import cl.turnojusto.empleados.comun.Pagina;
import cl.turnojusto.empleados.empleado.dto.DesactivacionRequest;
import cl.turnojusto.empleados.empleado.dto.EmpleadoRequest;
import cl.turnojusto.empleados.empleado.dto.EmpleadoResponse;
import cl.turnojusto.empleados.empleado.dto.PerfilResponse;
import cl.turnojusto.empleados.empleado.dto.ResumenResponse;
import cl.turnojusto.empleados.empleado.dto.TrasladoRequest;
import cl.turnojusto.empleados.empleado.dto.TrasladoResponse;
import cl.turnojusto.empleados.seguridad.ContextoActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API REST de empleados: {@code /api/v1/empleados}. */
@RestController
@RequestMapping("/api/v1/empleados")
@Tag(name = "Empleados", description = "Fichas, traslados y desactivación (F3)")
@SecurityRequirement(name = "entra")
public class EmpleadoController {

    private final EmpleadoService servicio;

    public EmpleadoController(EmpleadoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/me")
    @Operation(summary = "Contexto del usuario autenticado (empresa, sucursal y rol)")
    public PerfilResponse perfil() {
        return PerfilResponse.de(ContextoActual.obtener());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','COORDINADOR')")
    @Operation(summary = "Buscar empleados por texto, sucursal, equipo y estado")
    public Pagina<EmpleadoResponse> buscar(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) Long sucursalId,
                                           @RequestParam(required = false) Long equipoId,
                                           @RequestParam(required = false) EstadoEmpleado estado,
                                           @RequestParam(defaultValue = "0") int pagina,
                                           @RequestParam(defaultValue = "20") int tamano) {
        return servicio.buscar(ContextoActual.obtener(), q, sucursalId, equipoId, estado, pagina, tamano);
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','COORDINADOR')")
    @Operation(summary = "Totales de empleados activos e inactivos de la empresa")
    public ResumenResponse resumen() {
        return servicio.resumen(ContextoActual.obtener());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver la ficha de un empleado")
    public EmpleadoResponse obtener(@PathVariable Long id) {
        return servicio.obtener(ContextoActual.obtener(), id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Registrar un empleado")
    public ResponseEntity<EmpleadoResponse> crear(@Valid @RequestBody EmpleadoRequest req) {
        EmpleadoResponse creado = servicio.crear(ContextoActual.obtener(), req);
        return ResponseEntity.created(URI.create("/api/v1/empleados/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Editar la ficha de un empleado")
    public EmpleadoResponse actualizar(@PathVariable Long id, @Valid @RequestBody EmpleadoRequest req) {
        return servicio.actualizar(ContextoActual.obtener(), id, req);
    }

    @PostMapping("/{id}/traslados")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Trasladar a otra sucursal o equipo (publica EmpleadoTrasladado)")
    public EmpleadoResponse trasladar(@PathVariable Long id, @Valid @RequestBody TrasladoRequest req) {
        return servicio.trasladar(ContextoActual.obtener(), id, req);
    }

    @GetMapping("/{id}/traslados")
    @Operation(summary = "Historial de traslados del empleado")
    public List<TrasladoResponse> traslados(@PathVariable Long id) {
        return servicio.historialTraslados(ContextoActual.obtener(), id);
    }

    @PostMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Desactivar (sale de la rotación sin borrar su historial)")
    public EmpleadoResponse desactivar(@PathVariable Long id, @Valid @RequestBody DesactivacionRequest req) {
        return servicio.desactivar(ContextoActual.obtener(), id, req);
    }

    @PostMapping("/{id}/reactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Reactivar un empleado inactivo")
    public EmpleadoResponse reactivar(@PathVariable Long id) {
        return servicio.reactivar(ContextoActual.obtener(), id);
    }
}
