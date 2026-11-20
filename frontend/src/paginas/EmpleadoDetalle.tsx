import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import CalendarIcon from '@mui/icons-material/EventOutlined'
import EditIcon from '@mui/icons-material/EditOutlined'
import MailIcon from '@mui/icons-material/MailOutline'
import PersonOffIcon from '@mui/icons-material/PersonOffOutlined'
import PhoneIcon from '@mui/icons-material/PhoneOutlined'
import SwapIcon from '@mui/icons-material/SwapHoriz'
import {
  Alert, Box, Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, Paper, Skeleton, Stack, Tab,
  Table, TableBody, TableCell, TableHead, TableRow, Tabs, TextField, Typography,
} from '@mui/material'
import { useEffect, useMemo, useState } from 'react'
import { Link as RouterLink, useParams, useSearchParams } from 'react-router-dom'
import { useDesactivar, useEmpleado, useEquipos, useReactivar, useSucursales, useTraslados, useTrasladar } from '../api/hooks'
import type { Empleado, Perfil } from '../api/tipos'
import { AvatarPersona, EstadoChip, Kpi, Vacio } from '../componentes/Comunes'
import { useAvisar } from '../componentes/Notificador'
import { fecha, hoyIso, nombreRol } from '../util/formato'
import { formatearRut } from '../util/rut'
import { EmpleadoPanel } from './EmpleadoPanel'

function TrasladoDialogo({ empleado, abierto, alCerrar }: { empleado: Empleado; abierto: boolean; alCerrar: () => void }) {
  const { data: sucursales } = useSucursales()
  const { data: equipos } = useEquipos()
  const trasladar = useTrasladar()
  const avisar = useAvisar()
  const [f, setF] = useState({ sucursalDestinoId: empleado.sucursalId, equipoDestinoId: '' as number | '', fechaEfectiva: hoyIso(), motivo: '' })
  const [error, setError] = useState<string | null>(null)
  useEffect(() => { if (abierto) { setF({ sucursalDestinoId: empleado.sucursalId, equipoDestinoId: '', fechaEfectiva: hoyIso(), motivo: '' }); setError(null) } }, [abierto, empleado])
  return (
    <Dialog open={abierto} onClose={alCerrar} fullWidth maxWidth="sm">
      <DialogTitle>Trasladar a {empleado.nombres} {empleado.apellidos}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          <Stack direction="row" spacing={2}>
            <TextField select label="Sucursal de destino" value={f.sucursalDestinoId} onChange={(e) => setF({ ...f, sucursalDestinoId: Number(e.target.value), equipoDestinoId: '' })}>
              {sucursales?.filter((s) => s.estado === 'ACTIVA').map((s) => <MenuItem key={s.id} value={s.id}>{s.nombre}</MenuItem>)}
            </TextField>
            <TextField select label="Equipo de destino" value={f.equipoDestinoId} onChange={(e) => setF({ ...f, equipoDestinoId: e.target.value === '' ? '' : Number(e.target.value) })}>
              <MenuItem value="">Sin equipo</MenuItem>
              {equipos?.filter((q) => q.sucursalId === f.sucursalDestinoId && q.estado === 'ACTIVO').map((q) => <MenuItem key={q.id} value={q.id}>{q.nombre}</MenuItem>)}
            </TextField>
          </Stack>
          <TextField label="Fecha efectiva" type="date" value={f.fechaEfectiva} onChange={(e) => setF({ ...f, fechaEfectiva: e.target.value })} slotProps={{ inputLabel: { shrink: true } }} />
          <TextField label="Motivo" value={f.motivo} onChange={(e) => setF({ ...f, motivo: e.target.value })} />
          <Alert severity="info">El traslado queda en el historial y se publica el evento EmpleadoTrasladado para que los acumulados de equidad se calculen por sucursal.</Alert>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cancelar</Button>
        <Button variant="contained" disabled={trasladar.isPending} onClick={() => trasladar.mutate({
          id: empleado.id, datos: { ...f, equipoDestinoId: f.equipoDestinoId === '' ? null : f.equipoDestinoId },
        }, { onSuccess: () => { avisar('Traslado registrado'); alCerrar() }, onError: (e) => setError(e.message) })}>Trasladar</Button>
      </DialogActions>
    </Dialog>
  )
}

function DesactivarDialogo({ empleado, abierto, alCerrar }: { empleado: Empleado; abierto: boolean; alCerrar: () => void }) {
  const desactivar = useDesactivar()
  const avisar = useAvisar()
  const [f, setF] = useState({ fecha: hoyIso(), motivo: '' })
  const [error, setError] = useState<string | null>(null)
  useEffect(() => { if (abierto) { setF({ fecha: hoyIso(), motivo: '' }); setError(null) } }, [abierto])
  return (
    <Dialog open={abierto} onClose={alCerrar} fullWidth maxWidth="xs">
      <DialogTitle>Desactivar a {empleado.nombres}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          <Typography variant="body2">Deja de contar para la rotación desde la fecha indicada. Su historial no se borra y se puede reactivar.</Typography>
          <TextField label="Fecha" type="date" value={f.fecha} onChange={(e) => setF({ ...f, fecha: e.target.value })} slotProps={{ inputLabel: { shrink: true } }} />
          <TextField label="Motivo" required value={f.motivo} onChange={(e) => setF({ ...f, motivo: e.target.value })} />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cancelar</Button>
        <Button color="error" variant="contained" disabled={!f.motivo.trim() || desactivar.isPending}
          onClick={() => desactivar.mutate({ id: empleado.id, ...f }, { onSuccess: () => { avisar('Empleado desactivado'); alCerrar() }, onError: (e) => setError(e.message) })}>
          Desactivar
        </Button>
      </DialogActions>
    </Dialog>
  )
}

/** P05 · Ficha del empleado con acciones, resumen e historial de traslados. */
export function EmpleadoDetalle({ perfil, idPropio }: { perfil: Perfil; idPropio?: number }) {
  const params = useParams()
  const id = idPropio ?? Number(params.id)
  const [busqueda, setBusqueda] = useSearchParams()
  const admin = perfil.rol === 'ADMINISTRADOR'
  const { data: e, isLoading, error } = useEmpleado(id)
  const { data: traslados } = useTraslados(id)
  const { data: sucursales } = useSucursales()
  const { data: equipos } = useEquipos()
  const reactivar = useReactivar()
  const avisar = useAvisar()
  const [tab, setTab] = useState(0)
  const [editar, setEditar] = useState(false)
  const accion = busqueda.get('accion')
  const cerrarAccion = () => setBusqueda({})
  const sucursal = useMemo(() => new Map(sucursales?.map((s) => [s.id, s.nombre])), [sucursales])
  const equipo = useMemo(() => new Map(equipos?.map((q) => [q.id, q.nombre])), [equipos])

  if (isLoading) return <Skeleton height={300} />
  if (error || !e) return <Alert severity="error">{error?.message ?? 'No se encontró la ficha'}</Alert>
  const nombre = `${e.nombres} ${e.apellidos}`
  const antiguedadDias = Math.max(0, Math.round((Date.now() - new Date(e.fechaIngresoRotacion).getTime()) / 86400000))

  return (
    <>
      {!idPropio && <Button component={RouterLink} to="/empleados" startIcon={<ArrowBackIcon />} sx={{ mb: 2 }}>Empleados</Button>}
      <Paper sx={{ p: 3, mb: 2 }}>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} justifyContent="space-between">
          <Stack direction="row" spacing={2.5} alignItems="center">
            <AvatarPersona nombre={nombre} tamano={64} />
            <Box>
              <Stack direction="row" spacing={1} alignItems="center">
                <Typography variant="h5" component="h1">{nombre}</Typography>
                <EstadoChip estado={e.estado} />
                {!e.cuentaVinculada && <Chip size="small" label="Sin cuenta aún" variant="outlined" />}
              </Stack>
              <Typography color="text.secondary">
                {e.cargo ?? nombreRol[e.rol]} · {e.equipoId ? `Equipo ${equipo.get(e.equipoId)}` : 'Sin equipo'} · Sucursal {sucursal.get(e.sucursalId)} · RUT {formatearRut(e.rut)}
              </Typography>
              <Stack direction="row" spacing={2} sx={{ mt: 0.5, color: 'text.secondary' }} useFlexGap flexWrap="wrap">
                <Typography variant="body2"><MailIcon sx={{ fontSize: 16, verticalAlign: 'middle', mr: 0.5 }} />{e.correo}</Typography>
                {e.telefono && <Typography variant="body2"><PhoneIcon sx={{ fontSize: 16, verticalAlign: 'middle', mr: 0.5 }} />{e.telefono}</Typography>}
                <Typography variant="body2"><CalendarIcon sx={{ fontSize: 16, verticalAlign: 'middle', mr: 0.5 }} />En rotación desde {fecha(e.fechaIngresoRotacion)}</Typography>
              </Stack>
            </Box>
          </Stack>
          {admin && (
            <Stack direction="row" spacing={1} alignItems="flex-start">
              <Button variant="outlined" startIcon={<EditIcon />} onClick={() => setEditar(true)}>Editar</Button>
              {e.estado === 'ACTIVO' ? (
                <>
                  <Button variant="outlined" startIcon={<SwapIcon />} onClick={() => setBusqueda({ accion: 'trasladar' })}>Trasladar</Button>
                  <Button variant="outlined" color="error" startIcon={<PersonOffIcon />} disabled={e.id === perfil.empleadoId}
                    onClick={() => setBusqueda({ accion: 'desactivar' })}>Desactivar</Button>
                </>
              ) : (
                <Button variant="contained" onClick={() => reactivar.mutate(e.id, { onSuccess: () => avisar('Empleado reactivado'), onError: (x) => avisar(x.message, 'error') })}>Reactivar</Button>
              )}
            </Stack>
          )}
        </Stack>
      </Paper>
      {e.estado === 'INACTIVO' && <Alert severity="warning" sx={{ mb: 2 }}>Inactivo desde el {fecha(e.fechaDesactivacion)}. Motivo: {e.motivoDesactivacion}</Alert>}
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }}>
        <Tab label="Resumen" />
        <Tab label={`Traslados (${traslados?.length ?? 0})`} />
      </Tabs>
      {tab === 0 ? (
        <>
          <Stack direction="row" spacing={2} useFlexGap flexWrap="wrap" sx={{ mb: 2 }}>
            <Kpi icono={<CalendarIcon fontSize="small" />} titulo="Días en la rotación" valor={antiguedadDias} detalle={`desde ${fecha(e.fechaIngresoRotacion)}`} />
            <Kpi icono={<SwapIcon fontSize="small" />} titulo="Traslados" valor={traslados?.length ?? 0} detalle="cambios de sucursal o equipo" />
            <Kpi icono={<PersonOffIcon fontSize="small" />} titulo="Rol" valor={<Typography variant="h5" component="span">{nombreRol[e.rol]}</Typography>} detalle={e.cuentaVinculada ? 'cuenta vinculada a Entra' : 'aún no inicia sesión'} />
          </Stack>
          <Alert severity="info">Los turnos cubiertos, la cuota esperada (e = T × c / Σc) y la desviación se mostrarán aquí cuando se publique el primer calendario (Sprint 2).</Alert>
        </>
      ) : (
        <Paper>
          {!traslados?.length ? <Vacio texto="No registra traslados." /> : (
            <Table>
              <TableHead><TableRow><TableCell>Fecha efectiva</TableCell><TableCell>Desde</TableCell><TableCell>Hacia</TableCell><TableCell>Motivo</TableCell></TableRow></TableHead>
              <TableBody>
                {traslados.map((t) => (
                  <TableRow key={t.id}>
                    <TableCell>{fecha(t.fechaEfectiva)}</TableCell>
                    <TableCell>{sucursal.get(t.sucursalOrigenId)}{t.equipoOrigenId ? ` · ${equipo.get(t.equipoOrigenId)}` : ''}</TableCell>
                    <TableCell>{sucursal.get(t.sucursalDestinoId)}{t.equipoDestinoId ? ` · ${equipo.get(t.equipoDestinoId)}` : ''}</TableCell>
                    <TableCell>{t.motivo ?? '—'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </Paper>
      )}
      {admin && (
        <>
          <EmpleadoPanel abierto={editar} empleado={e} alCerrar={() => setEditar(false)} />
          <TrasladoDialogo empleado={e} abierto={accion === 'trasladar' && e.estado === 'ACTIVO'} alCerrar={cerrarAccion} />
          <DesactivarDialogo empleado={e} abierto={accion === 'desactivar' && e.estado === 'ACTIVO'} alCerrar={cerrarAccion} />
        </>
      )}
    </>
  )
}
