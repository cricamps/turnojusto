/** Tipos de las respuestas de ms-organizacion y ms-empleados. */
export type Rol = 'ADMINISTRADOR' | 'COORDINADOR' | 'EMPLEADO'
export type EstadoEmpleado = 'ACTIVO' | 'INACTIVO'

export interface Perfil {
  empleadoId: number
  empresaId: number
  sucursalId: number
  equipoId: number | null
  rol: Rol
  nombre: string
  correo: string
}

export interface Empresa {
  id: number
  rut: string
  razonSocial: string
  nombreFantasia: string | null
  correoContacto: string
  telefono: string | null
  estado: string
  version: number
}

export interface Region {
  codigo: string
  nombre: string
}

export interface Sucursal {
  id: number
  empresaId: number
  codigo: string
  nombre: string
  direccion: string
  comuna: string
  regionCodigo: string
  horaApertura: string
  horaCierre: string
  estado: 'ACTIVA' | 'INACTIVA'
  equipos: number
  version: number
}

export type SucursalForm = Pick<Sucursal, 'codigo' | 'nombre' | 'direccion' | 'comuna' | 'regionCodigo' | 'horaApertura' | 'horaCierre'>

export interface Equipo {
  id: number
  sucursalId: number
  nombre: string
  descripcion: string | null
  estado: 'ACTIVO' | 'INACTIVO'
  coordinadorEmpleadoId: number | null
  coordinadorDesde: string | null
  version: number
}

export interface Empleado {
  id: number
  empresaId: number
  sucursalId: number
  equipoId: number | null
  rut: string
  nombres: string
  apellidos: string
  correo: string
  telefono: string | null
  cargo: string | null
  rol: Rol
  fechaIngresoRotacion: string
  estado: EstadoEmpleado
  fechaDesactivacion: string | null
  motivoDesactivacion: string | null
  cuentaVinculada: boolean
  version: number
}

export interface EmpleadoForm {
  rut: string
  nombres: string
  apellidos: string
  correo: string
  telefono: string
  cargo: string
  rol: Rol
  sucursalId: number | ''
  equipoId: number | ''
  fechaIngresoRotacion: string
}

export interface Traslado {
  id: number
  sucursalOrigenId: number
  sucursalDestinoId: number
  equipoOrigenId: number | null
  equipoDestinoId: number | null
  fechaEfectiva: string
  motivo: string | null
}

export interface Pagina<T> {
  contenido: T[]
  total: number
  pagina: number
  tamano: number
  totalPaginas: number
}
