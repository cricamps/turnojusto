import { formatearRut, normalizarRut, rutValido } from './rut'

describe('RUT', () => {
  it('normaliza puntos, guion y k minúscula', () => {
    expect(normalizarRut('12.345.678-5')).toBe('12345678-5')
    expect(normalizarRut('10000013k')).toBe('10000013-K')
  })
  it('valida el dígito verificador', () => {
    expect(rutValido('12.345.678-5')).toBe(true)
    expect(rutValido('76123456-0')).toBe(true)
    expect(rutValido('10000013-K')).toBe(true)
    expect(rutValido('12.345.678-9')).toBe(false)
    expect(rutValido('abc')).toBe(false)
  })
  it('formatea con puntos', () => {
    expect(formatearRut('12345678-5')).toBe('12.345.678-5')
    expect(formatearRut('7654321-6')).toBe('7.654.321-6')
  })
})
