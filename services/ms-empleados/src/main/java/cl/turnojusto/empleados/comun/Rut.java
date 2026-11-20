package cl.turnojusto.empleados.comun;

/**
 * Utilidades para el RUT chileno.
 *
 * <p>El RUT se guarda normalizado, sin puntos y con guion: {@code 12345678-5}. El dígito
 * verificador se calcula con el algoritmo de módulo 11.</p>
 */
public final class Rut {

    private Rut() {
    }

    /**
     * Quita puntos y espacios, pasa la K a mayúscula y agrega el guion antes del dígito
     * verificador. Devuelve {@code null} si la entrada es nula o vacía.
     */
    public static String normalizar(String rut) {
        if (rut == null || rut.isBlank()) {
            return null;
        }
        String limpio = rut.replace(".", "").replace("-", "").replace(" ", "").toUpperCase();
        if (limpio.length() < 2) {
            return limpio;
        }
        return limpio.substring(0, limpio.length() - 1) + "-" + limpio.charAt(limpio.length() - 1);
    }

    /** Calcula el dígito verificador (0-9 o K) del cuerpo numérico del RUT. */
    public static char calcularDv(long cuerpo) {
        long suma = 0;
        int multiplicador = 2;
        while (cuerpo > 0) {
            suma += (cuerpo % 10) * multiplicador;
            cuerpo /= 10;
            multiplicador = multiplicador == 7 ? 2 : multiplicador + 1;
        }
        long resto = 11 - (suma % 11);
        if (resto == 11) {
            return '0';
        }
        if (resto == 10) {
            return 'K';
        }
        return (char) ('0' + resto);
    }

    /** Indica si el RUT (con o sin puntos) tiene formato y dígito verificador válidos. */
    public static boolean esValido(String rut) {
        String normalizado = normalizar(rut);
        if (normalizado == null || !normalizado.matches("^[1-9][0-9]{6,7}-[0-9K]$")) {
            return false;
        }
        long cuerpo = Long.parseLong(normalizado.substring(0, normalizado.indexOf('-')));
        return calcularDv(cuerpo) == normalizado.charAt(normalizado.length() - 1);
    }
}
