package cl.turnojusto.empleados.integracion;

import java.util.Optional;

/**
 * Puerto de salida hacia el proveedor de identidad (IDaaS). La ficha del empleado vive en
 * Oracle; aquí solo se crea la cuenta con la que la persona inicia sesión.
 */
public interface ProveedorIdentidad {

    /**
     * Crea (invita) la cuenta del correo indicado y devuelve su Object ID, o vacío si el
     * proveedor no está configurado o no respondió (la cuenta se vincula al primer ingreso).
     */
    Optional<String> crearCuenta(String correo, String nombre);
}
