package cl.turnojusto.organizacion.equipo;

import cl.turnojusto.organizacion.equipo.EquipoDtos.CoordinacionResponse;
import cl.turnojusto.organizacion.equipo.EquipoDtos.CoordinadorRequest;
import cl.turnojusto.organizacion.equipo.EquipoDtos.EquipoRequest;
import cl.turnojusto.organizacion.equipo.EquipoDtos.EquipoResponse;
import cl.turnojusto.organizacion.seguridad.ContextoActual;
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

/** API REST de equipos: {@code /api/v1/equipos}. */
@RestController
@RequestMapping("/api/v1/equipos")
@Tag(name = "Equipos", description = "Equipos de cada sucursal y su coordinador (F4)")
@SecurityRequirement(name = "entra")
public class EquipoController {

    private final EquipoService servicio;

    public EquipoController(EquipoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Listar equipos (el Administrador puede filtrar por sucursal)")
    public List<EquipoResponse> listar(@RequestParam(required = false) Long sucursalId) {
        return servicio.listar(ContextoActual.obtener(), sucursalId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver un equipo con su coordinador vigente")
    public EquipoResponse obtener(@PathVariable Long id) {
        return servicio.obtener(ContextoActual.obtener(), id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Crear un equipo en una sucursal")
    public ResponseEntity<EquipoResponse> crear(@Valid @RequestBody EquipoRequest req) {
        EquipoResponse creado = servicio.crear(ContextoActual.obtener(), req);
        return ResponseEntity.created(URI.create("/api/v1/equipos/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Editar nombre y descripción de un equipo")
    public EquipoResponse actualizar(@PathVariable Long id, @Valid @RequestBody EquipoRequest req) {
        return servicio.actualizar(ContextoActual.obtener(), id, req);
    }

    @PostMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Desactivar un equipo")
    public EquipoResponse desactivar(@PathVariable Long id) {
        return servicio.cambiarEstado(ContextoActual.obtener(), id, false);
    }

    @PostMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Activar un equipo")
    public EquipoResponse activar(@PathVariable Long id) {
        return servicio.cambiarEstado(ContextoActual.obtener(), id, true);
    }

    @PutMapping("/{id}/coordinador")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Asignar el coordinador del equipo (cierra la vigencia del anterior)")
    public EquipoResponse asignarCoordinador(@PathVariable Long id, @Valid @RequestBody CoordinadorRequest req) {
        return servicio.asignarCoordinador(ContextoActual.obtener(), id, req.empleadoId());
    }

    @GetMapping("/{id}/coordinadores")
    @Operation(summary = "Historial de coordinadores del equipo")
    public List<CoordinacionResponse> historial(@PathVariable Long id) {
        return servicio.historialCoordinadores(ContextoActual.obtener(), id);
    }
}
