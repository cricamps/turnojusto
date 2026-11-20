package cl.turnojusto.organizacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio ms-organizacion de TurnoJusto.
 *
 * <p>Es dueño del esquema ORG en Oracle Autonomous Database: datos de la empresa (tenant),
 * sus sucursales (dirección, comuna, región y horario), los equipos de cada sucursal y el
 * coordinador vigente de cada equipo. Funcionalidades F2 y F4 del producto.</p>
 */
@SpringBootApplication
public class MsOrganizacionApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsOrganizacionApplication.class, args);
    }
}
