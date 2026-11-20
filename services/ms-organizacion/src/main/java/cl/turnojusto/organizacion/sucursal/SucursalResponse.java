package cl.turnojusto.organizacion.sucursal;

/** Sucursal tal como la recibe el frontend. */
public record SucursalResponse(Long id, Long empresaId, String codigo, String nombre, String direccion, String comuna,
                               String regionCodigo, String horaApertura, String horaCierre, String estado,
                               long equipos, Long version) {

    static SucursalResponse de(Sucursal s, long equipos) {
        return new SucursalResponse(s.getId(), s.getEmpresaId(), s.getCodigo(), s.getNombre(), s.getDireccion(),
                s.getComuna(), s.getRegionCodigo(), s.getHoraApertura(), s.getHoraCierre(), s.getEstado(), equipos,
                s.getVersion());
    }
}
