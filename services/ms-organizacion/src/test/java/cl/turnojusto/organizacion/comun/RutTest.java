package cl.turnojusto.organizacion.comun;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RutTest {

    @Test
    @DisplayName("Normaliza un RUT con puntos y k minúscula")
    void normaliza() {
        assertThat(Rut.normalizar("12.345.678-5")).isEqualTo("12345678-5");
        assertThat(Rut.normalizar("10.000.013-k")).isEqualTo("10000013-K");
        assertThat(Rut.normalizar("123456785")).isEqualTo("12345678-5");
        assertThat(Rut.normalizar("  ")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12.345.678-5", "76123456-0", "11111111-1", "10000013-K"})
    @DisplayName("Acepta RUT con dígito verificador correcto")
    void aceptaValidos(String rut) {
        assertThat(Rut.esValido(rut)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12.345.678-9", "1-9", "abc", "", "012345678-5", "123456789-0"})
    @DisplayName("Rechaza RUT inválidos (CP-21)")
    void rechazaInvalidos(String rut) {
        assertThat(Rut.esValido(rut)).isFalse();
    }

    @Test
    void calculaDvK() {
        assertThat(Rut.calcularDv(10000013)).isEqualTo('K');
    }
}
