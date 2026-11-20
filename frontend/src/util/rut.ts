/** Validación del RUT chileno en el formulario (la API lo vuelve a validar). */
export function normalizarRut(rut: string): string {
  const limpio = rut.replace(/[.\s-]/g, '').toUpperCase()
  return limpio.length < 2 ? limpio : `${limpio.slice(0, -1)}-${limpio.slice(-1)}`
}

export function calcularDv(cuerpo: number): string {
  let suma = 0
  let m = 2
  for (const d of String(cuerpo).split('').reverse()) {
    suma += Number(d) * m
    m = m === 7 ? 2 : m + 1
  }
  const r = 11 - (suma % 11)
  return r === 11 ? '0' : r === 10 ? 'K' : String(r)
}

export function rutValido(rut: string): boolean {
  const n = normalizarRut(rut)
  if (!/^[1-9]\d{6,7}-[\dK]$/.test(n)) return false
  const [cuerpo, dv] = n.split('-')
  return calcularDv(Number(cuerpo)) === dv
}

/** 12345678-5 → 12.345.678-5 */
export function formatearRut(rut: string): string {
  const n = normalizarRut(rut)
  const [cuerpo, dv] = n.split('-')
  if (!dv) return rut
  return `${cuerpo.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}-${dv}`
}
