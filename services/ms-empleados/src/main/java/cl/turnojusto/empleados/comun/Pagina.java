package cl.turnojusto.empleados.comun;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Página de resultados con un formato JSON estable para el frontend. */
public record Pagina<T>(List<T> contenido, long total, int pagina, int tamano, int totalPaginas) {

    public static <E, T> Pagina<T> de(Page<E> page, Function<E, T> mapeo) {
        return new Pagina<>(page.getContent().stream().map(mapeo).toList(), page.getTotalElements(),
                page.getNumber(), page.getSize(), page.getTotalPages());
    }
}
