import AddIcon from '@mui/icons-material/PersonAddAlt1Outlined'
import ClearIcon from '@mui/icons-material/Close'
import MoreIcon from '@mui/icons-material/MoreVert'
import SearchIcon from '@mui/icons-material/Search'
import {
  Button, Chip, IconButton, InputAdornment, LinearProgress, Menu, MenuItem, Paper, Stack, Table, TableBody, TableCell,
  TableHead, TablePagination, TableRow, TextField, Typography,
} from '@mui/material'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useEmpleados, useEquipos, useSucursales } from '../api/hooks'
import type { Empleado, Perfil } from '../api/tipos'
import { AvatarPersona, Encabezado, EstadoChip, Vacio } from '../componentes/Comunes'
import { fecha, nombreRol } from '../util/formato'
import { formatearRut } from '../util/rut'
import { EmpleadoPanel } from './EmpleadoPanel'

/** P04 · Búsqueda de empleados con filtros (F3). */
export function Empleados({ perfil }: { perfil: Perfil }) {
  const admin = perfil.rol === 'ADMINISTRADOR'
  const [params] = useSearchParams()
  const navegar = useNavigate()
  const [texto, setTexto] = useState('')
  const [q, setQ] = useState('')
  const [sucursalId, setSucursalId] = useState<number | ''>(params.get('sucursal') ? Number(params.get('sucursal')) : '')
  const [equipoId, setEquipoId] = useState<number | ''>('')
  const [estado, setEstado] = useState<'' | 'ACTIVO' | 'INACTIVO'>('ACTIVO')
  const [pagina, setPagina] = useState(0)
  const [tamano, setTamano] = useState(25)
  const [panel, setPanel] = useState<{ abierto: boolean; empleado: Empleado | null }>({ abierto: false, empleado: null })
  const [menu, setMenu] = useState<{ el: HTMLElement; e: Empleado } | null>(null)

  // Búsqueda incremental con espera de 300 ms (desde 2 caracteres).
  useEffect(() => {
    const t = setTimeout(() => { setQ(texto); setPagina(0) }, 300)
    return () => clearTimeout(t)
  }, [texto])

  const { data, isFetching } = useEmpleados({ q, sucursalId, equipoId, estado, pagina, tamano })
  const { data: sucursales } = useSucursales()
  const { data: equipos } = useEquipos()
  const nombreSucursal = useMemo(() => new Map(sucursales?.map((s) => [s.id, s.nombre])), [sucursales])
  const nombreEquipo = useMemo(() => new Map(equipos?.map((e) => [e.id, e.nombre])), [equipos])
  const filtrosActivos = [
    sucursalId !== '' && { k: 'suc', t: `Sucursal: ${nombreSucursal.get(sucursalId)}`, quitar: () => setSucursalId('') },
    equipoId !== '' && { k: 'eq', t: `Equipo: ${nombreEquipo.get(equipoId)}`, quitar: () => setEquipoId('') },
    estado !== '' && { k: 'est', t: `Estado: ${estado === 'ACTIVO' ? 'Activos' : 'Inactivos'}`, quitar: () => setEstado('') },
  ].filter(Boolean) as { k: string; t: string; quitar: () => void }[]

  return (
    <>
      <Encabezado titulo="Empleados"
        subtitulo={data ? `${data.total} resultado(s)${admin ? '' : ` · sucursal ${nombreSucursal.get(perfil.sucursalId) ?? ''}`}` : undefined}
        acciones={admin && <Button variant="contained" startIcon={<AddIcon />} onClick={() => setPanel({ abierto: true, empleado: null })}>Nuevo empleado</Button>} />
      <Paper>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5} sx={{ p: 2 }}>
          <TextField placeholder="Buscar por nombre, RUT o correo" value={texto} onChange={(e) => setTexto(e.target.value)} sx={{ flex: 2 }}
            slotProps={{ input: { startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> } }} />
          {admin && (
            <TextField select label="Sucursal" value={sucursalId} sx={{ flex: 1 }} onChange={(e) => { setSucursalId(e.target.value === '' ? '' : Number(e.target.value)); setEquipoId(''); setPagina(0) }}>
              <MenuItem value="">Todas</MenuItem>
              {sucursales?.map((s) => <MenuItem key={s.id} value={s.id}>{s.nombre}</MenuItem>)}
            </TextField>
          )}
          <TextField select label="Equipo" value={equipoId} sx={{ flex: 1 }} onChange={(e) => { setEquipoId(e.target.value === '' ? '' : Number(e.target.value)); setPagina(0) }}>
            <MenuItem value="">Todos</MenuItem>
            {equipos?.filter((e) => sucursalId === '' || e.sucursalId === sucursalId).map((e) => <MenuItem key={e.id} value={e.id}>{e.nombre} · {nombreSucursal.get(e.sucursalId)}</MenuItem>)}
          </TextField>
          <TextField select label="Estado" value={estado} sx={{ flex: 1 }} onChange={(e) => { setEstado(e.target.value as typeof estado); setPagina(0) }}>
            <MenuItem value="">Todos</MenuItem><MenuItem value="ACTIVO">Activos</MenuItem><MenuItem value="INACTIVO">Inactivos</MenuItem>
          </TextField>
        </Stack>
        {filtrosActivos.length > 0 && (
          <Stack direction="row" spacing={1} sx={{ px: 2, pb: 1.5 }} alignItems="center">
            {filtrosActivos.map((c) => <Chip key={c.k} size="small" color="primary" variant="outlined" label={c.t} onDelete={c.quitar} />)}
            <Button size="small" startIcon={<ClearIcon />} onClick={() => { setSucursalId(''); setEquipoId(''); setEstado(''); setTexto('') }}>Limpiar filtros</Button>
          </Stack>
        )}
        {isFetching && <LinearProgress sx={{ height: 2 }} />}
        {data && !data.contenido.length ? <Vacio texto="No hay empleados que coincidan con la búsqueda." /> : (
          <Table>
            <TableHead>
              <TableRow><TableCell>Nombre</TableCell><TableCell>Equipo</TableCell><TableCell>Sucursal</TableCell><TableCell>Rol</TableCell>
                <TableCell>Ingreso a rotación</TableCell><TableCell>Estado</TableCell><TableCell /></TableRow>
            </TableHead>
            <TableBody>
              {data?.contenido.map((e) => (
                <TableRow key={e.id} hover sx={{ cursor: 'pointer' }} onClick={() => navegar(`/empleados/${e.id}`)}>
                  <TableCell>
                    <Stack direction="row" spacing={1.5} alignItems="center">
                      <AvatarPersona nombre={`${e.nombres} ${e.apellidos}`} />
                      <div><Typography sx={{ fontWeight: 600 }}>{e.nombres} {e.apellidos}</Typography>
                        <Typography variant="caption" color="text.secondary" sx={{ fontFamily: 'monospace' }}>{formatearRut(e.rut)}</Typography></div>
                    </Stack>
                  </TableCell>
                  <TableCell>{e.equipoId ? nombreEquipo.get(e.equipoId) : '—'}</TableCell>
                  <TableCell>{nombreSucursal.get(e.sucursalId)}</TableCell>
                  <TableCell>{nombreRol[e.rol]}</TableCell>
                  <TableCell>{fecha(e.fechaIngresoRotacion)}</TableCell>
                  <TableCell><EstadoChip estado={e.estado} /></TableCell>
                  <TableCell align="right" onClick={(ev) => ev.stopPropagation()}>
                    <IconButton aria-label="Acciones" onClick={(ev) => setMenu({ el: ev.currentTarget, e })}><MoreIcon /></IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
        <TablePagination component="div" count={data?.total ?? 0} page={pagina} rowsPerPage={tamano}
          onPageChange={(_, p) => setPagina(p)} onRowsPerPageChange={(e) => { setTamano(Number(e.target.value)); setPagina(0) }}
          rowsPerPageOptions={[10, 25, 50]} labelRowsPerPage="Filas por página:" />
      </Paper>
      <Menu anchorEl={menu?.el} open={!!menu} onClose={() => setMenu(null)}>
        <MenuItem onClick={() => { navegar(`/empleados/${menu!.e.id}`); setMenu(null) }}>Ver ficha</MenuItem>
        {admin && <MenuItem onClick={() => { setPanel({ abierto: true, empleado: menu!.e }); setMenu(null) }}>Editar datos</MenuItem>}
        {admin && <MenuItem onClick={() => { navegar(`/empleados/${menu!.e.id}?accion=trasladar`); setMenu(null) }}>Trasladar de sucursal</MenuItem>}
        {admin && menu?.e.estado === 'ACTIVO' && <MenuItem sx={{ color: 'error.main' }} onClick={() => { navegar(`/empleados/${menu!.e.id}?accion=desactivar`); setMenu(null) }}>Desactivar…</MenuItem>}
      </Menu>
      <EmpleadoPanel abierto={panel.abierto} empleado={panel.empleado} alCerrar={() => setPanel({ abierto: false, empleado: null })} />
    </>
  )
}
