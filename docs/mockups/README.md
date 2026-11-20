# Mockups de TurnoJusto

| Versión | Carpeta | Origen |
|---|---|---|
| v1 | [`v1/`](v1) | Mockups de alta fidelidad P01–P10 y mapa de navegación, entregados en la semana 4. |
| v2 | [`v2/`](v2) | Pantallas reales del Sprint 1, capturadas en https://turnojusto.xkcx.cl con los datos de la semilla QA (20-11-2026). |

Las pantallas v2 conservan el código de v1 cuando corresponden a la misma pantalla (P01–P05). Las que no tenían mockup en v1 usan el prefijo `N`. P06 a P10 (turnos, calendario, reemplazos, reportes y vista móvil) siguen en v1 porque se construyen en los Sprints 2 y 3.

## Cambios de v1 a v2

### Cambios generales (todas las pantallas)

- **Menú por sprint:** las opciones de Turnos, Calendario, Reemplazos y Reportes se muestran deshabilitadas, con una etiqueta S2 o S3. Así cada rol ve lo que viene sin llegar a pantallas vacías.
- **Barra superior:** migas de pan más un chip con el alcance de los datos («Todas las sucursales» para el Administrador o la sucursal del Coordinador). Se quitaron la búsqueda global y la campana de avisos (Sprint 3).
- **Identidad:** el pie del menú dice «Sesión protegida con Microsoft Entra ID». El inquilino final es Entra ID (workforce) porque Azure for Students no permitió crear Entra External ID.
- **Accesibilidad:** el RUT se muestra en fuente monoespaciada. Los estados usan un texto («Activo», «Inactiva») además del color, y todos los botones tienen texto o etiqueta.

### Pantallas existentes

| Pantalla | v1 (semana 4) | v2 (Sprint 1) | Motivo |
|---|---|---|---|
| P01 Inicio de sesión | Paso de código MFA dibujado dentro de la pantalla e ilustración de calendario. | Un solo botón «Ingresar con mi cuenta». MFA y contraseña ocurren en la página de Microsoft. El texto explica que primero el administrador registra a la persona. | TurnoJusto no debe pedir ni mostrar credenciales: todo el flujo de autenticación lo maneja el IDaaS (OIDC + PKCE). |
| P02 Inicio (Administrador) | Indicadores de calendario y desviación de equidad. | Indicadores del Sprint 1: sucursales activas, empleados en rotación, equipos activos (y cuántos no tienen coordinador) y fichas inactivas. «Estado por sucursal» muestra horario, equipos y personas. «Requiere tu atención» avisa de equipos sin coordinador. | Mostrar solo datos reales disponibles en este sprint. Los indicadores de equidad vuelven cuando exista el calendario (Sprint 2). |
| P03 Formulario de sucursal | Panel lateral con validación en línea. | Se mantiene el patrón de panel lateral. Se agregan el texto de ayuda «Único en la empresa» para el código, la región como lista desde Oracle (`ORG.REGION`) y un aviso de que los feriados regionales se aplican en el Sprint 2. | Retroalimentación: dejar claras las reglas de negocio en el mismo formulario. |
| P04 Búsqueda de empleados | Importar CSV, selección múltiple y columna «Turnos 2026». | Búsqueda por nombre, RUT o correo con filtros de sucursal, equipo y estado mostrados como chips, más un botón «Limpiar filtros». Columna «Ingreso a rotación». Menú de acciones por fila. | Se quitan funciones que dependen de turnos o de cargas masivas (fuera del Sprint 1). |
| P05 Detalle de empleado | Seis pestañas (disponibilidad, turnos, reemplazos, traslados, historial). | Dos pestañas: Resumen y Traslados. Etiqueta «Sin cuenta aún» mientras la persona no haya ingresado con Entra. Tarjetas de días en rotación, traslados y rol. Aviso de cómo se calculará la cuota de equidad (e = T × c / Σc). | La ficha muestra lo que el Sprint 1 ya guarda. Las demás pestañas llegan con los turnos. |

### Pantallas nuevas

| Pantalla | Qué resuelve |
|---|---|
| N01 Listado de sucursales | Pestaña «Sucursales» de Empresa y sucursales: dirección, región, horario, número de equipos, estado y acciones de editar y desactivar. |
| N02 Alta de empleado con validaciones | Resumen de errores arriba del formulario («Revisa 4 campo(s) antes de guardar»), mensajes por campo y validación del dígito verificador del RUT antes de llamar a la API. |
| N03 Equipos por sucursal | Equipos agrupados por sucursal, con el coordinador vigente y la fecha desde la que lo es, integrantes y estado. |
| N04 Asignar coordinador | Diálogo que solo ofrece personas activas de la misma sucursal con rol Coordinador o Administrador, y advierte que la vigencia anterior queda en el historial. |
| N05 Traslado | Sucursal y equipo de destino, fecha efectiva y motivo. Indica que se publica el evento `EmpleadoTrasladado` para los acumulados de equidad. |
