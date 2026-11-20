import { MenuItem, Stack, TextField } from '@mui/material'
import { useEffect, useState } from 'react'
import { ErrorApi } from '../api/cliente'
import { useEquipos, useGuardarEmpleado, useSucursales } from '../api/hooks'
import type { Empleado, EmpleadoForm } from '../api/tipos'
import { useAvisar } from '../componentes/Notificador'
import { PanelLateral } from '../componentes/PanelLateral'
import { hoyIso, nombreRol } from '../util/formato'
import { normalizarRut, rutValido } from '../util/rut'

const vacio = (): EmpleadoForm => ({
  rut: '', nombres: '', apellidos: '', correo: '', telefono: '', cargo: '', rol: 'EMPLEADO', sucursalId: '', equipoId: '', fechaIngresoRotacion: hoyIso(),
})

/** Reglas del formulario P03; las mismas se validan en ms-empleados y en Oracle. */
export function validarEmpleado(f: EmpleadoForm): Record<string, string> {
  const e: Record<string, string> = {}
  if (!f.rut.trim()) e.rut = 'El RUT es obligatorio'
  else if (!rutValido(f.rut)) e.rut = 'El RUT no es válido (revisa el dígito verificador)'
  if (!f.nombres.trim()) e.nombres = 'Los nombres son obligatorios'
  if (!f.apellidos.trim()) e.apellidos = 'Los apellidos son obligatorios'
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(f.correo.trim())) e.correo = 'Ingresa un correo válido'
  if (f.telefono && !/^\+?[0-9 ]{8,15}$/.test(f.telefono)) e.telefono = 'El teléfono no es válido'
  if (f.sucursalId === '') e.sucursalId = 'Selecciona la sucursal'
  if (!f.fechaIngresoRotacion) e.fechaIngresoRotacion = 'Indica la fecha de ingreso'
  else if (f.fechaIngresoRotacion > hoyIso()) e.fechaIngresoRotacion = 'No puede ser una fecha futura'
  return e
}

/** P03 · Formulario de empleado en panel lateral. */
export function EmpleadoPanel({ abierto, empleado, alCerrar }: { abierto: boolean; empleado: Empleado | null; alCerrar: () => void }) {
  const [f, setF] = useState<EmpleadoForm>(vacio())
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const { data: sucursales } = useSucursales()
  const { data: equipos } = useEquipos()
  const guardar = useGuardarEmpleado()
  const avisar = useAvisar()

  useEffect(() => {
    if (!abierto) return
    setErrores({})
    setError(null)
    setF(empleado ? {
      rut: empleado.rut, nombres: empleado.nombres, apellidos: empleado.apellidos, correo: empleado.correo,
      telefono: empleado.telefono ?? '', cargo: empleado.cargo ?? '', rol: empleado.rol, sucursalId: empleado.sucursalId,
      equipoId: empleado.equipoId ?? '', fechaIngresoRotacion: empleado.fechaIngresoRotacion,
    } : vacio())
  }, [abierto, empleado])

  const campo = (k: keyof EmpleadoForm) => ({
    value: f[k],
    onChange: (e: React.ChangeEvent<HTMLInputElement>) => setF({ ...f, [k]: e.target.value }),
    error: !!errores[k],
    helperText: errores[k],
  })

  const alGuardar = () => {
    const e = validarEmpleado(f)
    setErrores(e)
    if (Object.keys(e).length) return setError(`Revisa ${Object.keys(e).length} campo(s) antes de guardar.`)
    guardar.mutate({ id: empleado?.id, datos: { ...f, rut: normalizarRut(f.rut) } }, {
      onSuccess: () => { avisar(empleado ? 'Ficha actualizada' : 'Empleado registrado. Podrá ingresar creando su cuenta con ese correo.'); alCerrar() },
      onError: (err) => { setError(err.message); if (err instanceof ErrorApi) setErrores(err.campos) },
    })
  }

  return (
    <PanelLateral abierto={abierto} titulo={empleado ? `Editar a ${empleado.nombres}` : 'Nuevo empleado'} error={error}
      guardando={guardar.isPending} textoGuardar={empleado ? 'Guardar cambios' : 'Registrar empleado'} alCerrar={alCerrar} alGuardar={alGuardar}>
      <TextField label="RUT" required placeholder="12.345.678-5" {...campo('rut')}
        onBlur={() => f.rut && setErrores({ ...errores, rut: rutValido(f.rut) ? '' : 'El RUT no es válido (revisa el dígito verificador)' })} />
      <Stack direction="row" spacing={2}>
        <TextField label="Nombres" required {...campo('nombres')} />
        <TextField label="Apellidos" required {...campo('apellidos')} />
      </Stack>
      <TextField label="Correo" required type="email" {...campo('correo')}
        helperText={errores.correo ?? 'Con este correo la persona crea su cuenta e inicia sesión'} />
      <Stack direction="row" spacing={2}>
        <TextField label="Teléfono" placeholder="+56 9 1234 5678" {...campo('telefono')} />
        <TextField label="Cargo" {...campo('cargo')} />
      </Stack>
      <TextField select label="Rol" required {...campo('rol')}>
        {(['EMPLEADO', 'COORDINADOR', 'ADMINISTRADOR'] as const).map((r) => <MenuItem key={r} value={r}>{nombreRol[r]}</MenuItem>)}
      </TextField>
      <Stack direction="row" spacing={2}>
        <TextField select label="Sucursal" required {...campo('sucursalId')} disabled={!!empleado}
          onChange={(e) => setF({ ...f, sucursalId: Number(e.target.value), equipoId: '' })}
          helperText={errores.sucursalId ?? (empleado ? 'Para cambiarla usa Trasladar' : undefined)}>
          {sucursales?.filter((s) => s.estado === 'ACTIVA' || s.id === f.sucursalId).map((s) => <MenuItem key={s.id} value={s.id}>{s.nombre}</MenuItem>)}
        </TextField>
        <TextField select label="Equipo" {...campo('equipoId')} disabled={!!empleado || f.sucursalId === ''}>
          <MenuItem value="">Sin equipo</MenuItem>
          {equipos?.filter((q) => q.sucursalId === f.sucursalId && q.estado === 'ACTIVO').map((q) => <MenuItem key={q.id} value={q.id}>{q.nombre}</MenuItem>)}
        </TextField>
      </Stack>
      <TextField label="Ingreso a la rotación" type="date" required {...campo('fechaIngresoRotacion')}
        slotProps={{ inputLabel: { shrink: true }, htmlInput: { max: hoyIso() } }}
        helperText={errores.fechaIngresoRotacion ?? 'Desde esta fecha cuenta para el reparto equitativo de turnos'} />
    </PanelLateral>
  )
}
