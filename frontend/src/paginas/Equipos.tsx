import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/EditOutlined'
import PersonIcon from '@mui/icons-material/ManageAccountsOutlined'
import {
  Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, IconButton, MenuItem, Paper, Skeleton, Stack,
  Table, TableBody, TableCell, TableHead, TableRow, TextField, Tooltip, Typography,
} from '@mui/material'
import { useEffect, useMemo, useState } from 'react'
import { useAsignarCoordinador, useEmpleados, useEquipos, useEstadoEquipo, useGuardarEquipo, useSucursales } from '../api/hooks'
import type { Empleado, Equipo, Perfil } from '../api/tipos'
import { AvatarPersona, Encabezado, EstadoChip, Vacio } from '../componentes/Comunes'
import { useAvisar } from '../componentes/Notificador'
import { PanelLateral } from '../componentes/PanelLateral'
import { fecha } from '../util/formato'

function EquipoPanel({ abierto, equipo, sucursalInicial, alCerrar }: { abierto: boolean; equipo: Equipo | null; sucursalInicial?: number; alCerrar: () => void }) {
  const { data: sucursales } = useSucursales()
  const guardar = useGuardarEquipo()
  const avisar = useAvisar()
  const [f, setF] = useState({ sucursalId: 0, nombre: '', descripcion: '' })
  const [error, setError] = useState<string | null>(null)
  useEffect(() => {
    if (abierto) {
      setF(equipo ? { sucursalId: equipo.sucursalId, nombre: equipo.nombre, descripcion: equipo.descripcion ?? '' }
        : { sucursalId: sucursalInicial ?? sucursales?.find((s) => s.estado === 'ACTIVA')?.id ?? 0, nombre: '', descripcion: '' })
      setError(null)
    }
  }, [abierto, equipo, sucursalInicial, sucursales])
  return (
    <PanelLateral abierto={abierto} titulo={equipo ? `Editar ${equipo.nombre}` : 'Nuevo equipo'} error={error}
      guardando={guardar.isPending} textoGuardar="Guardar equipo" alCerrar={alCerrar}
      alGuardar={() => {
        if (!f.nombre.trim() || !f.sucursalId) return setError('Indica la sucursal y el nombre del equipo.')
        guardar.mutate({ id: equipo?.id, datos: f }, {
          onSuccess: () => { avisar(equipo ? 'Equipo actualizado' : 'Equipo creado'); alCerrar() },
          onError: (e) => setError(e.message),
        })
      }}>
      <TextField select label="Sucursal" required value={f.sucursalId || ''} disabled={!!equipo}
        onChange={(e) => setF({ ...f, sucursalId: Number(e.target.value) })}
        helperText={equipo ? 'Un equipo no cambia de sucursal' : 'Solo sucursales activas'}>
        {sucursales?.filter((s) => s.estado === 'ACTIVA' || s.id === equipo?.sucursalId).map((s) => <MenuItem key={s.id} value={s.id}>{s.nombre}</MenuItem>)}
      </TextField>
      <TextField label="Nombre del equipo" required value={f.nombre} onChange={(e) => setF({ ...f, nombre: e.target.value })}
        helperText="No puede repetirse dentro de la misma sucursal" />
      <TextField label="Descripción" multiline minRows={3} value={f.descripcion} onChange={(e) => setF({ ...f, descripcion: e.target.value })} />
    </PanelLateral>
  )
}

function CoordinadorDialogo({ equipo, candidatos, alCerrar }: { equipo: Equipo | null; candidatos: Empleado[]; alCerrar: () => void }) {
  const [empleadoId, setEmpleadoId] = useState<number | ''>('')
  const asignar = useAsignarCoordinador()
  const avisar = useAvisar()
  const [error, setError] = useState<string | null>(null)
  useEffect(() => { setEmpleadoId(equipo?.coordinadorEmpleadoId ?? ''); setError(null) }, [equipo])
  const opciones = candidatos.filter((e) => e.sucursalId === equipo?.sucursalId && e.rol !== 'EMPLEADO' && e.estado === 'ACTIVO')
  return (
    <Dialog open={!!equipo} onClose={alCerrar} fullWidth maxWidth="xs">
      <DialogTitle>Coordinador de {equipo?.nombre}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          <TextField select label="Coordinador" value={empleadoId} onChange={(e) => setEmpleadoId(Number(e.target.value))}
            helperText="Personas activas de la sucursal con rol Coordinador o Administrador">
            {opciones.map((e) => <MenuItem key={e.id} value={e.id}>{e.nombres} {e.apellidos} · {e.cargo ?? e.rol}</MenuItem>)}
          </TextField>
          {!opciones.length && <Alert severity="info">No hay coordinadores en esta sucursal. Asigna el rol Coordinador a un empleado.</Alert>}
          <Typography variant="caption" color="text.secondary">Al cambiar de coordinador se cierra la vigencia del anterior y queda en el historial.</Typography>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cancelar</Button>
        <Button variant="contained" disabled={!empleadoId || asignar.isPending}
          onClick={() => equipo && empleadoId && asignar.mutate({ id: equipo.id, empleadoId }, {
            onSuccess: () => { avisar('Coordinador asignado'); alCerrar() },
            onError: (e) => setError(e.message),
          })}>Asignar</Button>
      </DialogActions>
    </Dialog>
  )
}

/** Equipos por sucursal y su coordinador (funcionalidad F4). */
export function Equipos({ perfil }: { perfil: Perfil }) {
  const admin = perfil.rol === 'ADMINISTRADOR'
  const { data: equipos, isLoading } = useEquipos()
  const { data: sucursales } = useSucursales()
  const { data: empleados } = useEmpleados({ estado: 'ACTIVO', tamano: 100 })
  const estado = useEstadoEquipo()
  const avisar = useAvisar()
  const [panel, setPanel] = useState<{ abierto: boolean; equipo: Equipo | null; sucursal?: number }>({ abierto: false, equipo: null })
  const [coordinar, setCoordinar] = useState<Equipo | null>(null)
  const personas = useMemo(() => new Map((empleados?.contenido ?? []).map((e) => [e.id, e])), [empleados])
  const integrantes = (id: number) => empleados?.contenido.filter((e) => e.equipoId === id).length ?? 0

  return (
    <>
      <Encabezado titulo="Equipos" subtitulo={admin ? 'Equipos de cada sucursal y su coordinador vigente' : 'Equipos de tu sucursal'}
        acciones={admin && <Button variant="contained" startIcon={<AddIcon />} onClick={() => setPanel({ abierto: true, equipo: null })}>Nuevo equipo</Button>} />
      {isLoading && <Skeleton height={240} />}
      <Stack spacing={2}>
        {sucursales?.map((s) => {
          const lista = equipos?.filter((e) => e.sucursalId === s.id) ?? []
          return (
            <Paper key={s.id}>
              <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ px: 2.5, py: 1.5, borderBottom: '1px solid #e2e8f0' }}>
                <Typography variant="h6">{s.nombre} <Typography component="span" variant="caption" color="text.secondary">{s.codigo} · {s.horaApertura}–{s.horaCierre}</Typography></Typography>
                {admin && s.estado === 'ACTIVA' && <Button size="small" startIcon={<AddIcon />} onClick={() => setPanel({ abierto: true, equipo: null, sucursal: s.id })}>Agregar equipo</Button>}
              </Stack>
              {!lista.length ? <Vacio texto="Esta sucursal aún no tiene equipos." /> : (
                <Table size="small">
                  <TableHead><TableRow><TableCell>Equipo</TableCell><TableCell>Coordinador</TableCell><TableCell align="right">Integrantes</TableCell><TableCell>Estado</TableCell>{admin && <TableCell />}</TableRow></TableHead>
                  <TableBody>
                    {lista.map((e) => {
                      const c = e.coordinadorEmpleadoId ? personas.get(e.coordinadorEmpleadoId) : undefined
                      return (
                        <TableRow key={e.id} hover>
                          <TableCell><Typography sx={{ fontWeight: 600 }}>{e.nombre}</Typography><Typography variant="caption" color="text.secondary">{e.descripcion}</Typography></TableCell>
                          <TableCell>
                            {c ? (
                              <Stack direction="row" spacing={1} alignItems="center">
                                <AvatarPersona nombre={`${c.nombres} ${c.apellidos}`} tamano={26} />
                                <span>{c.nombres} {c.apellidos}<Typography variant="caption" display="block" color="text.secondary">desde {fecha(e.coordinadorDesde)}</Typography></span>
                              </Stack>
                            ) : <Typography variant="body2" color="warning.main">Sin coordinador</Typography>}
                          </TableCell>
                          <TableCell align="right">{integrantes(e.id)}</TableCell>
                          <TableCell><EstadoChip estado={e.estado} /></TableCell>
                          {admin && (
                            <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                              <Tooltip title="Asignar coordinador"><IconButton onClick={() => setCoordinar(e)}><PersonIcon fontSize="small" /></IconButton></Tooltip>
                              <Tooltip title="Editar"><IconButton onClick={() => setPanel({ abierto: true, equipo: e })}><EditIcon fontSize="small" /></IconButton></Tooltip>
                              <Button size="small" color={e.estado === 'ACTIVO' ? 'error' : 'primary'}
                                onClick={() => estado.mutate({ id: e.id, activo: e.estado !== 'ACTIVO' }, { onSuccess: () => avisar('Estado del equipo actualizado'), onError: (x) => avisar(x.message, 'error') })}>
                                {e.estado === 'ACTIVO' ? 'Desactivar' : 'Activar'}
                              </Button>
                            </TableCell>
                          )}
                        </TableRow>
                      )
                    })}
                  </TableBody>
                </Table>
              )}
            </Paper>
          )
        })}
      </Stack>
      <EquipoPanel abierto={panel.abierto} equipo={panel.equipo} sucursalInicial={panel.sucursal} alCerrar={() => setPanel({ abierto: false, equipo: null })} />
      <CoordinadorDialogo equipo={coordinar} candidatos={empleados?.contenido ?? []} alCerrar={() => setCoordinar(null)} />
    </>
  )
}
