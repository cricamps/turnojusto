package cl.turnojusto.empleados.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicadorOutboxTest {

    @Mock
    OutboxRepository repositorio;
    @InjectMocks
    PublicadorOutbox publicador;

    @Test
    void marcaComoPublicadosLosPendientes() {
        OutboxEvento e = new OutboxEvento(1000L, "EmpleadoTrasladado", 5L, "{\"empleadoId\":5}");
        when(repositorio.findTop20ByEstadoOrderByCreadoEnAsc("PENDIENTE")).thenReturn(List.of(e));
        publicador.publicarPendientes();
        assertThat(e.getEstado()).isEqualTo("PUBLICADO");
    }

    @Test
    void trasCincoFallosQuedaEnError() {
        OutboxEvento e = new OutboxEvento(1000L, "X", 1L, "{}");
        for (int i = 0; i < 5; i++) {
            e.registrarFallo();
        }
        assertThat(e.getEstado()).isEqualTo("ERROR");
        assertThat(e.getId()).hasSize(36);
    }
}
