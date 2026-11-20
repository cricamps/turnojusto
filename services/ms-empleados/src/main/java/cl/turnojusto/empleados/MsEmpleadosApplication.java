package cl.turnojusto.empleados;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Microservicio ms-empleados de TurnoJusto.
 *
 * <p>Es dueño del esquema EMP en Oracle Autonomous Database y administra la ficha de cada
 * trabajador: sucursal y equipo actuales, fecha de ingreso a la rotación, traslados entre
 * sucursales y desactivación. También resuelve el <em>contexto</em> del usuario autenticado
 * (empresa, sucursal y rol) a partir del token de Microsoft Entra External ID; el resto de
 * los microservicios lo consulta en {@code GET /api/v1/empleados/me}.</p>
 */
@SpringBootApplication
@EnableScheduling
public class MsEmpleadosApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsEmpleadosApplication.class, args);
    }
}
