import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './cliente'
import type {
  Empleado, EmpleadoForm, Empresa, Equipo, Pagina, Perfil, Region, Sucursal, SucursalForm, Traslado,
} from './tipos'

/** Consultas y mutaciones de la API con caché de TanStack Query. */
export const usePerfil = (habilitado: boolean) =>
  useQuery({ queryKey: ['perfil'], queryFn: () => api<Perfil>('/api/v1/empleados/me'), enabled: habilitado, retry: false })

export const useEmpresa = () => useQuery({ queryKey: ['empresa'], queryFn: () => api<Empresa>('/api/v1/empresa') })
export const useRegiones = () =>
  useQuery({ queryKey: ['regiones'], queryFn: () => api<Region[]>('/api/v1/regiones'), staleTime: Infinity })
export const useSucursales = () => useQuery({ queryKey: ['sucursales'], queryFn: () => api<Sucursal[]>('/api/v1/sucursales') })
export const useEquipos = () => useQuery({ queryKey: ['equipos'], queryFn: () => api<Equipo[]>('/api/v1/equipos') })
export const useResumenEmpleados = (habilitado = true) =>
  useQuery({
    queryKey: ['empleados', 'resumen'],
    queryFn: () => api<{ activos: number; inactivos: number }>('/api/v1/empleados/resumen'),
    enabled: habilitado,
  })

export interface FiltroEmpleados {
  q?: string
  sucursalId?: number | ''
  equipoId?: number | ''
  estado?: '' | 'ACTIVO' | 'INACTIVO'
  pagina?: number
  tamano?: number
}

export const useEmpleados = (f: FiltroEmpleados, habilitado = true) => {
  const p = new URLSearchParams()
  if (f.q && f.q.trim().length >= 2) p.set('q', f.q.trim())
  if (f.sucursalId) p.set('sucursalId', String(f.sucursalId))
  if (f.equipoId) p.set('equipoId', String(f.equipoId))
  if (f.estado) p.set('estado', f.estado)
  p.set('pagina', String(f.pagina ?? 0))
  p.set('tamano', String(f.tamano ?? 25))
  return useQuery({
    queryKey: ['empleados', p.toString()],
    queryFn: () => api<Pagina<Empleado>>(`/api/v1/empleados?${p}`),
    enabled: habilitado,
    placeholderData: (anterior) => anterior,
  })
}

export const useEmpleado = (id: number) =>
  useQuery({ queryKey: ['empleado', id], queryFn: () => api<Empleado>(`/api/v1/empleados/${id}`) })
export const useTraslados = (id: number) =>
  useQuery({ queryKey: ['empleado', id, 'traslados'], queryFn: () => api<Traslado[]>(`/api/v1/empleados/${id}/traslados`) })

/** Mutación que invalida las consultas indicadas al terminar bien. */
function useAccion<V, R>(fn: (v: V) => Promise<R>, invalidar: string[][]) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: () => invalidar.forEach((k) => qc.invalidateQueries({ queryKey: k })),
  })
}

export const useGuardarEmpresa = () =>
  useAccion((e: Pick<Empresa, 'razonSocial' | 'nombreFantasia' | 'correoContacto' | 'telefono'>) =>
    api<Empresa>('/api/v1/empresa', { metodo: 'PUT', cuerpo: e }), [['empresa']])

export const useGuardarSucursal = () =>
  useAccion(({ id, datos }: { id?: number; datos: SucursalForm }) =>
    api<Sucursal>(id ? `/api/v1/sucursales/${id}` : '/api/v1/sucursales', { metodo: id ? 'PUT' : 'POST', cuerpo: datos }),
  [['sucursales']])

export const useEstadoSucursal = () =>
  useAccion(({ id, activa }: { id: number; activa: boolean }) =>
    api<Sucursal>(`/api/v1/sucursales/${id}/${activa ? 'activar' : 'desactivar'}`, { metodo: 'POST' }), [['sucursales']])

export const useGuardarEquipo = () =>
  useAccion(({ id, datos }: { id?: number; datos: { sucursalId: number; nombre: string; descripcion: string } }) =>
    api<Equipo>(id ? `/api/v1/equipos/${id}` : '/api/v1/equipos', { metodo: id ? 'PUT' : 'POST', cuerpo: datos }),
  [['equipos'], ['sucursales']])

export const useEstadoEquipo = () =>
  useAccion(({ id, activo }: { id: number; activo: boolean }) =>
    api<Equipo>(`/api/v1/equipos/${id}/${activo ? 'activar' : 'desactivar'}`, { metodo: 'POST' }), [['equipos'], ['sucursales']])

export const useAsignarCoordinador = () =>
  useAccion(({ id, empleadoId }: { id: number; empleadoId: number }) =>
    api<Equipo>(`/api/v1/equipos/${id}/coordinador`, { metodo: 'PUT', cuerpo: { empleadoId } }), [['equipos']])

const aCuerpo = (f: EmpleadoForm) => ({
  ...f,
  sucursalId: f.sucursalId === '' ? null : f.sucursalId,
  equipoId: f.equipoId === '' ? null : f.equipoId,
})

export const useGuardarEmpleado = () =>
  useAccion(({ id, datos }: { id?: number; datos: EmpleadoForm }) =>
    api<Empleado>(id ? `/api/v1/empleados/${id}` : '/api/v1/empleados', { metodo: id ? 'PUT' : 'POST', cuerpo: aCuerpo(datos) }),
  [['empleados'], ['empleado']])

export const useTrasladar = () =>
  useAccion(({ id, datos }: { id: number; datos: { sucursalDestinoId: number; equipoDestinoId: number | null; fechaEfectiva: string; motivo: string } }) =>
    api<Empleado>(`/api/v1/empleados/${id}/traslados`, { metodo: 'POST', cuerpo: datos }), [['empleados'], ['empleado']])

export const useDesactivar = () =>
  useAccion(({ id, fecha, motivo }: { id: number; fecha: string; motivo: string }) =>
    api<Empleado>(`/api/v1/empleados/${id}/desactivar`, { metodo: 'POST', cuerpo: { fecha, motivo } }), [['empleados'], ['empleado']])

export const useReactivar = () =>
  useAccion((id: number) => api<Empleado>(`/api/v1/empleados/${id}/reactivar`, { metodo: 'POST' }), [['empleados'], ['empleado']])
