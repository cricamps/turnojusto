package cl.turnojusto.organizacion.sucursal;

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
import org.springframework.web.bind.annotation.RestController;

/** API REST de sucursales: {@code /api/v1/sucursales}. */
@RestController
@RequestMapping("/api/v1/sucursales")
@Tag(name = "Sucursales", description = "Registro de sucursales con dirección, región y horario (F2)")
@SecurityRequirement(name = "entra")
public class SucursalController {

    private final SucursalService servicio;

    public SucursalController(SucursalService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Listar las sucursales visibles para el usuario")
    public List<SucursalResponse> listar() {
        return servicio.listar(ContextoActual.obtener());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver una sucursal")
    public SucursalResponse obtener(@PathVariable Long id) {
        return servicio.obtener(ContextoActual.obtener(), id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Registrar una sucursal")
    public ResponseEntity<SucursalResponse> crear(@Valid @RequestBody SucursalRequest req) {
        SucursalResponse creada = servicio.crear(ContextoActual.obtener(), req);
        return ResponseEntity.created(URI.create("/api/v1/sucursales/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Editar una sucursal")
    public SucursalResponse actualizar(@PathVariable Long id, @Valid @RequestBody SucursalRequest req) {
        return servicio.actualizar(ContextoActual.obtener(), id, req);
    }

    @PostMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Desactivar una sucursal sin equipos activos")
    public SucursalResponse desactivar(@PathVariable Long id) {
        return servicio.cambiarEstado(ContextoActual.obtener(), id, false);
    }

    @PostMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Volver a activar una sucursal")
    public SucursalResponse activar(@PathVariable Long id) {
        return servicio.cambiarEstado(ContextoActual.obtener(), id, true);
    }
}
