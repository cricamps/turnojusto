package cl.turnojusto.organizacion.comun;

/** Se intentó una operación que viola una regla del negocio (responde 422). */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
