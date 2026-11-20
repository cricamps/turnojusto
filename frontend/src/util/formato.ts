import type { Rol } from '../api/tipos'

export const nombreRol: Record<Rol, string> = {
  ADMINISTRADOR: 'Administrador',
  COORDINADOR: 'Coordinador',
  EMPLEADO: 'Empleado',
}

/** 2026-11-20 → 20-11-2026 */
export const fecha = (iso: string | null | undefined) => (iso ? iso.split('-').reverse().join('-') : '—')

export const hoyIso = () => {
  const d = new Date()
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
}

export const iniciales = (nombre: string) =>
  nombre.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0]).join('').toUpperCase()

const colores = ['#0f766e', '#2563eb', '#db2777', '#ea580c', '#7c3aed', '#059669', '#b45309', '#475569']
export const colorAvatar = (texto: string) =>
  colores[[...texto].reduce((a, c) => a + c.charCodeAt(0), 0) % colores.length]

export const saludo = () => {
  const h = new Date().getHours()
  return h < 12 ? 'Buenos días' : h < 20 ? 'Buenas tardes' : 'Buenas noches'
}
