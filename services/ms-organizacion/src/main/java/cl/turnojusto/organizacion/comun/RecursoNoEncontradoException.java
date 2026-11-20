package cl.turnojusto.organizacion.comun;

/** El recurso pedido no existe o no pertenece a la empresa del usuario (responde 404). */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
