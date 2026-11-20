import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/EditOutlined'
import {
  Alert, Box, Button, IconButton, MenuItem, Paper, Skeleton, Stack, Tab, Table, TableBody, TableCell, TableHead,
  TableRow, Tabs, TextField, Tooltip, Typography,
} from '@mui/material'
import { useEffect, useState } from 'react'
import { ErrorApi } from '../api/cliente'
import {
  useEmpresa, useEstadoSucursal, useGuardarEmpresa, useGuardarSucursal, useRegiones, useSucursales,
} from '../api/hooks'
import type { Sucursal, SucursalForm } from '../api/tipos'
import { Encabezado, EstadoChip, Vacio } from '../componentes/Comunes'
import { useAvisar } from '../componentes/Notificador'
import { PanelLateral } from '../componentes/PanelLateral'
import { formatearRut } from '../util/rut'

const vacia: SucursalForm = { codigo: '', nombre: '', direccion: '', comuna: '', regionCodigo: 'CL-RM', horaApertura: '09:00', horaCierre: '21:00' }

/** Validación en el navegador; la API y la base de datos repiten las mismas reglas. */
export function validarSucursal(f: SucursalForm): Record<string, string> {
  const e: Record<string, string> = {}
  if (!/^[A-Za-z0-9]{2,10}$/.test(f.codigo.trim())) e.codigo = 'Usa de 2 a 10 letras o números'
  if (!f.nombre.trim()) e.nombre = 'Ingresa el nombre de la sucursal'
  if (!f.direccion.trim()) e.direccion = 'Ingresa la dirección de la sucursal'
  if (!f.comuna.trim()) e.comuna = 'Ingresa la comuna'
  if (!f.regionCodigo) e.regionCodigo = 'Selecciona la región'
  if (f.horaApertura && f.horaCierre && f.horaCierre <= f.horaApertura) e.horaCierre = 'Debe ser posterior a la apertura'
  return e
}

function SucursalPanel({ sucursal, abierto, alCerrar }: { sucursal: Sucursal | null; abierto: boolean; alCerrar: () => void }) {
  const [f, setF] = useState<SucursalForm>(vacia)
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const { data: regiones } = useRegiones()
  const guardar = useGuardarSucursal()
  const avisar = useAvisar()

  useEffect(() => {
    if (abierto) {
      setF(sucursal ? { ...sucursal } : vacia)
      setErrores({})
      setError(null)
    }
  }, [abierto, sucursal])

  const campo = (k: keyof SucursalForm) => ({
    value: f[k],
    onChange: (e: React.ChangeEvent<HTMLInputElement>) => setF({ ...f, [k]: e.target.value }),
    error: !!errores[k],
    helperText: errores[k],
  })

  const alGuardar = () => {
    const e = validarSucursal(f)
    setErrores(e)
    if (Object.keys(e).length) {
      setError(`Revisa ${Object.keys(e).length} campo(s) antes de guardar.`)
      return
    }
    guardar.mutate({ id: sucursal?.id, datos: { ...f, codigo: f.codigo.trim().toUpperCase() } }, {
      onSuccess: () => {
        avisar(sucursal ? 'Sucursal actualizada' : 'Sucursal registrada')
        alCerrar()
      },
      onError: (err) => {
        setError(err.message)
        if (err instanceof ErrorApi) setErrores(err.campos)
      },
    })
  }

  return (
    <PanelLateral abierto={abierto} titulo={sucursal ? `Editar ${sucursal.nombre}` : 'Nueva sucursal'} error={error}
      guardando={guardar.isPending} textoGuardar="Guardar sucursal" alCerrar={alCerrar} alGuardar={alGuardar}>
      <Stack direction="row" spacing={2}>
        <TextField label="Nombre" required {...campo('nombre')} sx={{ flex: 2 }} />
        <TextField label="Código" required {...campo('codigo')} sx={{ flex: 1 }}
          helperText={errores.codigo ?? 'Único en la empresa'} slotProps={{ htmlInput: { maxLength: 10 } }} />
      </Stack>
      <TextField label="Dirección" required placeholder="Calle y número" {...campo('direccion')} />
      <Stack direction="row" spacing={2}>
        <TextField select label="Región" required {...campo('regionCodigo')}>
          {regiones?.map((r) => <MenuItem key={r.codigo} value={r.codigo}>{r.nombre}</MenuItem>)}
        </TextField>
        <TextField label="Comuna" required {...campo('comuna')} />
      </Stack>
      <Stack direction="row" spacing={2}>
        <TextField label="Hora de apertura" type="time" required {...campo('horaApertura')} slotProps={{ inputLabel: { shrink: true } }} />
        <TextField label="Hora de cierre" type="time" required {...campo('horaCierre')} slotProps={{ inputLabel: { shrink: true } }} />
      </Stack>
      <Alert severity="info">Se aplicarán automáticamente los feriados nacionales y los de la región seleccionada (Sprint 2).</Alert>
    </PanelLateral>
  )
}

function DatosEmpresa() {
  const { data: empresa, isLoading } = useEmpresa()
  const guardar = useGuardarEmpresa()
  const avisar = useAvisar()
  const [f, setF] = useState({ razonSocial: '', nombreFantasia: '', correoContacto: '', telefono: '' })
  useEffect(() => {
    if (empresa) setF({ razonSocial: empresa.razonSocial, nombreFantasia: empresa.nombreFantasia ?? '', correoContacto: empresa.correoContacto, telefono: empresa.telefono ?? '' })
  }, [empresa])
  if (isLoading || !empresa) return <Skeleton height={240} />
  return (
    <Paper sx={{ p: 3, maxWidth: 640 }}>
      <Stack spacing={2}>
        <TextField label="RUT" value={formatearRut(empresa.rut)} disabled helperText="El RUT identifica a la empresa y no se puede editar" />
        <TextField label="Razón social" required value={f.razonSocial} onChange={(e) => setF({ ...f, razonSocial: e.target.value })} />
        <TextField label="Nombre de fantasía" value={f.nombreFantasia} onChange={(e) => setF({ ...f, nombreFantasia: e.target.value })} />
        <Stack direction="row" spacing={2}>
          <TextField label="Correo de contacto" required value={f.correoContacto} onChange={(e) => setF({ ...f, correoContacto: e.target.value })} />
          <TextField label="Teléfono" value={f.telefono} onChange={(e) => setF({ ...f, telefono: e.target.value })} />
        </Stack>
        <Box>
          <Button variant="contained" disabled={guardar.isPending}
            onClick={() => guardar.mutate(f, { onSuccess: () => avisar('Datos de la empresa guardados'), onError: (e) => avisar(e.message, 'error') })}>
            Guardar cambios
          </Button>
        </Box>
      </Stack>
    </Paper>
  )
}

/** Empresa y sucursales (funcionalidad F2, solo Administrador). */
export function Empresa() {
  const [tab, setTab] = useState(1)
  const [panel, setPanel] = useState<{ abierto: boolean; sucursal: Sucursal | null }>({ abierto: false, sucursal: null })
  const { data: empresa } = useEmpresa()
  const { data: sucursales, isLoading } = useSucursales()
  const { data: regiones } = useRegiones()
  const estado = useEstadoSucursal()
  const avisar = useAvisar()
  const region = (c: string) => regiones?.find((r) => r.codigo === c)?.nombre ?? c

  return (
    <>
      <Encabezado titulo="Empresa y sucursales"
        subtitulo={empresa ? `${empresa.nombreFantasia ?? empresa.razonSocial} · RUT ${formatearRut(empresa.rut)}` : undefined}
        acciones={<Button variant="contained" startIcon={<AddIcon />} onClick={() => { setTab(1); setPanel({ abierto: true, sucursal: null }) }}>Nueva sucursal</Button>} />
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }}>
        <Tab label="Datos de la empresa" />
        <Tab label={`Sucursales (${sucursales?.length ?? 0})`} />
      </Tabs>
      {tab === 0 ? <DatosEmpresa /> : (
        <Paper>
          {isLoading ? <Skeleton height={200} sx={{ m: 2 }} /> : !sucursales?.length ? (
            <Vacio texto="Aún no registras sucursales." accion={<Button variant="contained" onClick={() => setPanel({ abierto: true, sucursal: null })}>Registrar la primera</Button>} />
          ) : (
            <Table>
              <TableHead>
                <TableRow><TableCell>Sucursal</TableCell><TableCell>Dirección</TableCell><TableCell>Horario</TableCell>
                  <TableCell align="right">Equipos</TableCell><TableCell>Estado</TableCell><TableCell /></TableRow>
              </TableHead>
              <TableBody>
                {sucursales.map((s) => (
                  <TableRow key={s.id} hover>
                    <TableCell><Typography sx={{ fontWeight: 600 }}>{s.nombre}</Typography><Typography variant="caption" color="text.secondary" sx={{ fontFamily: 'monospace' }}>{s.codigo}</Typography></TableCell>
                    <TableCell>{s.direccion}, {s.comuna}<Typography variant="caption" display="block" color="text.secondary">{region(s.regionCodigo)}</Typography></TableCell>
                    <TableCell>{s.horaApertura} – {s.horaCierre}</TableCell>
                    <TableCell align="right">{s.equipos}</TableCell>
                    <TableCell><EstadoChip estado={s.estado} /></TableCell>
                    <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                      <Tooltip title="Editar"><IconButton onClick={() => setPanel({ abierto: true, sucursal: s })}><EditIcon fontSize="small" /></IconButton></Tooltip>
                      <Button size="small" color={s.estado === 'ACTIVA' ? 'error' : 'primary'}
                        onClick={() => estado.mutate({ id: s.id, activa: s.estado !== 'ACTIVA' }, {
                          onSuccess: () => avisar(s.estado === 'ACTIVA' ? 'Sucursal desactivada' : 'Sucursal activada'),
                          onError: (e) => avisar(e.message, 'error'),
                        })}>
                        {s.estado === 'ACTIVA' ? 'Desactivar' : 'Activar'}
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </Paper>
      )}
      <SucursalPanel abierto={panel.abierto} sucursal={panel.sucursal} alCerrar={() => setPanel({ abierto: false, sucursal: null })} />
    </>
  )
}
