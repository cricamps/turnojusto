package cl.turnojusto.organizacion.empresa;

import cl.turnojusto.organizacion.comun.RecursoNoEncontradoException;
import cl.turnojusto.organizacion.seguridad.ContextoActual;
import cl.turnojusto.organizacion.seguridad.ContextoUsuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Datos de la empresa del usuario: {@code /api/v1/empresa}. */
@RestController
@RequestMapping("/api/v1/empresa")
@Tag(name = "Empresa", description = "Datos de la empresa (F2)")
@SecurityRequirement(name = "entra")
public class EmpresaController {

    private final EmpresaRepository repositorio;

    public EmpresaController(EmpresaRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Empresa tal como la recibe el frontend. */
    public record EmpresaResponse(Long id, String rut, String razonSocial, String nombreFantasia,
                                  String correoContacto, String telefono, String estado, Long version) {
        static EmpresaResponse de(Empresa e) {
            return new EmpresaResponse(e.getId(), e.getRut(), e.getRazonSocial(), e.getNombreFantasia(),
                    e.getCorreoContacto(), e.getTelefono(), e.getEstado(), e.getVersion());
        }
    }

    /** Datos editables de la empresa. */
    public record EmpresaRequest(
            @NotBlank(message = "La razón social es obligatoria") @Size(max = 150) String razonSocial,
            @Size(max = 100) String nombreFantasia,
            @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido") String correoContacto,
            @Pattern(regexp = "^$|^\\+?[0-9 ]{8,15}$", message = "El teléfono no es válido") String telefono) {
    }

    @GetMapping
    @Operation(summary = "Ver los datos de mi empresa")
    @Transactional(readOnly = true)
    public EmpresaResponse obtener() {
        return EmpresaResponse.de(cargar(ContextoActual.obtener()));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Editar los datos de mi empresa")
    @Transactional
    public EmpresaResponse actualizar(@Valid @RequestBody EmpresaRequest req) {
        ContextoUsuario ctx = ContextoActual.obtener();
        Empresa e = cargar(ctx);
        e.actualizar(req.razonSocial().trim(), blanco(req.nombreFantasia()), req.correoContacto().trim().toLowerCase(),
                blanco(req.telefono()));
        e.auditar(ctx.oid());
        return EmpresaResponse.de(repositorio.save(e));
    }

    private Empresa cargar(ContextoUsuario ctx) {
        return repositorio.findById(ctx.empresaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La empresa no existe"));
    }

    private static String blanco(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
