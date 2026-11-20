package cl.turnojusto.empleados.comun;

/** El dato ya existe (por ejemplo, un RUT repetido en la empresa) y responde 409. */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
