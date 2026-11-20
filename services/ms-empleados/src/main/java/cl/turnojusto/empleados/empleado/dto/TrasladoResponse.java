package cl.turnojusto.empleados.empleado.dto;

import cl.turnojusto.empleados.empleado.Traslado;
import java.time.LocalDate;

/** Fila del historial de traslados. */
public record TrasladoResponse(Long id, Long sucursalOrigenId, Long sucursalDestinoId, Long equipoOrigenId,
                               Long equipoDestinoId, LocalDate fechaEfectiva, String motivo) {

    public static TrasladoResponse de(Traslado t) {
        return new TrasladoResponse(t.getId(), t.getSucursalOrigenId(), t.getSucursalDestinoId(),
                t.getEquipoOrigenId(), t.getEquipoDestinoId(), t.getFechaEfectiva(), t.getMotivo());
    }
}
