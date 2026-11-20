import ApartmentIcon from '@mui/icons-material/ApartmentOutlined'
import ChevronRightIcon from '@mui/icons-material/ChevronRight'
import GroupsIcon from '@mui/icons-material/GroupsOutlined'
import PeopleIcon from '@mui/icons-material/PeopleOutline'
import PersonOffIcon from '@mui/icons-material/PersonOffOutlined'
import {
  Alert, Box, Button, LinearProgress, Paper, Skeleton, Stack, Table, TableBody, TableCell, TableHead, TableRow,
  Typography,
} from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'
import { useEmpleados, useEquipos, useResumenEmpleados, useSucursales } from '../api/hooks'
import type { Perfil } from '../api/tipos'
import { EstadoChip, Kpi } from '../componentes/Comunes'
import { saludo } from '../util/formato'

/** P02 · Inicio: indicadores y estado por sucursal con los datos reales del Sprint 1. */
export function Inicio({ perfil }: { perfil: Perfil }) {
  const { data: sucursales, isLoading } = useSucursales()
  const { data: equipos } = useEquipos()
  const { data: resumen } = useResumenEmpleados()
  const { data: activos } = useEmpleados({ estado: 'ACTIVO', tamano: 100 })
  const porSucursal = new Map<number, number>()
  activos?.contenido.forEach((e) => porSucursal.set(e.sucursalId, (porSucursal.get(e.sucursalId) ?? 0) + 1))
  const maximo = Math.max(1, ...porSucursal.values())
  const sinEquipo = activos?.contenido.filter((e) => e.equipoId === null && e.rol !== 'ADMINISTRADOR').length ?? 0
  const sinCoordinador = equipos?.filter((e) => e.estado === 'ACTIVO' && !e.coordinadorEmpleadoId).length ?? 0
  const primerNombre = perfil.nombre.split(' ')[0]

  return (
    <>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" component="h1">{saludo()}, {primerNombre}</Typography>
        <Typography color="text.secondary">Resumen de tu {perfil.rol === 'ADMINISTRADOR' ? 'empresa' : 'sucursal'}</Typography>
      </Box>
      <Stack direction="row" spacing={2} useFlexGap flexWrap="wrap" sx={{ mb: 3 }}>
        <Kpi icono={<ApartmentIcon fontSize="small" />} titulo="Sucursales activas"
          valor={sucursales ? sucursales.filter((s) => s.estado === 'ACTIVA').length : '—'}
          detalle={sucursales?.map((s) => s.nombre).join(', ')} />
        <Kpi icono={<PeopleIcon fontSize="small" />} titulo="Empleados en rotación" valor={resumen?.activos ?? '—'}
          detalle={perfil.rol === 'ADMINISTRADOR' ? 'en toda la empresa' : 'en la empresa'} />
        <Kpi icono={<GroupsIcon fontSize="small" />} titulo="Equipos activos"
          valor={equipos ? equipos.filter((e) => e.estado === 'ACTIVO').length : '—'} detalle={`${sinCoordinador} sin coordinador`} />
        <Kpi icono={<PersonOffIcon fontSize="small" />} titulo="Fichas inactivas" valor={resumen?.inactivos ?? '—'}
          detalle="no cuentan para la rotación" />
      </Stack>
      <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { lg: '2fr 1fr' } }}>
        <Paper sx={{ p: 2.5 }}>
          <Typography variant="h6" sx={{ mb: 1 }}>Estado por sucursal</Typography>
          {isLoading ? <Skeleton height={160} /> : (
            <Table size="small">
              <TableHead>
                <TableRow><TableCell>Sucursal</TableCell><TableCell>Horario</TableCell><TableCell align="right">Equipos</TableCell>
                  <TableCell align="right">Personas</TableCell><TableCell>Estado</TableCell><TableCell /></TableRow>
              </TableHead>
              <TableBody>
                {sucursales?.map((s) => (
                  <TableRow key={s.id} hover>
                    <TableCell><b>{s.nombre}</b> <Typography component="span" variant="caption" color="text.secondary">{s.codigo}</Typography></TableCell>
                    <TableCell>{s.horaApertura}–{s.horaCierre}</TableCell>
                    <TableCell align="right">{s.equipos}</TableCell>
                    <TableCell align="right">{porSucursal.get(s.id) ?? 0}</TableCell>
                    <TableCell><EstadoChip estado={s.estado} /></TableCell>
                    <TableCell align="right">
                      <Button size="small" component={RouterLink} to={`/empleados?sucursal=${s.id}`} endIcon={<ChevronRightIcon />}>Ver personas</Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
          <Typography variant="body2" sx={{ fontWeight: 600, mt: 3, mb: 1 }}>Personas en rotación por sucursal</Typography>
          {sucursales?.map((s) => (
            <Stack key={s.id} direction="row" spacing={2} alignItems="center" sx={{ mb: 1 }}>
              <Typography variant="body2" sx={{ width: 160 }}>{s.nombre}</Typography>
              <LinearProgress variant="determinate" value={((porSucursal.get(s.id) ?? 0) / maximo) * 100}
                sx={{ flex: 1, height: 10, borderRadius: 5 }} aria-label={`Personas en ${s.nombre}`} />
              <Typography variant="body2" sx={{ width: 28, textAlign: 'right', fontWeight: 600 }}>{porSucursal.get(s.id) ?? 0}</Typography>
            </Stack>
          ))}
        </Paper>
        <Stack spacing={2}>
          <Paper sx={{ p: 2.5 }}>
            <Typography variant="h6" sx={{ mb: 1.5 }}>Requiere tu atención</Typography>
            <Stack spacing={1}>
              {sinCoordinador > 0 && <Alert severity="warning">{sinCoordinador} equipo(s) activo(s) sin coordinador asignado.</Alert>}
              {sinEquipo > 0 && <Alert severity="info">{sinEquipo} persona(s) activa(s) sin equipo asignado.</Alert>}
              {sinCoordinador === 0 && sinEquipo === 0 && <Alert severity="success">Todo en orden: cada equipo tiene coordinador.</Alert>}
            </Stack>
          </Paper>
          <Paper sx={{ p: 2.5 }}>
            <Typography variant="h6" sx={{ mb: 1 }}>Accesos rápidos</Typography>
            {perfil.rol === 'ADMINISTRADOR' && <Button fullWidth sx={{ justifyContent: 'space-between' }} component={RouterLink} to="/empresa" endIcon={<ChevronRightIcon />}>Registrar sucursal</Button>}
            <Button fullWidth sx={{ justifyContent: 'space-between' }} component={RouterLink} to="/equipos" endIcon={<ChevronRightIcon />}>
              {perfil.rol === 'ADMINISTRADOR' ? 'Crear equipo y asignar coordinador' : 'Ver equipos de mi sucursal'}
            </Button>
            <Button fullWidth sx={{ justifyContent: 'space-between' }} component={RouterLink} to="/empleados" endIcon={<ChevronRightIcon />}>
              {perfil.rol === 'ADMINISTRADOR' ? 'Registrar empleado' : 'Buscar empleados'}
            </Button>
          </Paper>
        </Stack>
      </Box>
    </>
  )
}
