package cl.turnojusto.organizacion.region;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Catálogo de regiones para los formularios de sucursal. */
@RestController
@RequestMapping("/api/v1/regiones")
@Tag(name = "Regiones")
public class RegionController {

    private final RegionRepository repositorio;

    public RegionController(RegionRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Región como la recibe el frontend. */
    public record RegionResponse(String codigo, String nombre) {
    }

    @GetMapping
    @Operation(summary = "Listar las regiones de Chile de norte a sur")
    public List<RegionResponse> listar() {
        return repositorio.findAllByOrderByOrdenAsc().stream()
                .map(r -> new RegionResponse(r.getCodigo(), r.getNombre())).toList();
    }
}
