import { validarEmpleado } from './EmpleadoPanel'
import { validarSucursal } from './Empresa'

describe('Validaciones de formularios', () => {
  it('CP-26: cierre anterior a la apertura', () => {
    const e = validarSucursal({ codigo: 'MAI', nombre: 'Maipú', direccion: 'Av. 1', comuna: 'Maipú', regionCodigo: 'CL-RM', horaApertura: '22:00', horaCierre: '10:00' })
    expect(e.horaCierre).toBe('Debe ser posterior a la apertura')
  })
  it('CP-21: campos obligatorios y RUT inválido del empleado', () => {
    const e = validarEmpleado({ rut: '12.345.678-9', nombres: '', apellidos: 'P', correo: 'x', telefono: '', cargo: '', rol: 'EMPLEADO', sucursalId: '', equipoId: '', fechaIngresoRotacion: '2026-01-01' })
    expect(Object.keys(e).sort()).toEqual(['correo', 'nombres', 'rut', 'sucursalId'])
  })
  it('acepta un empleado válido', () => {
    const e = validarEmpleado({ rut: '12.345.678-5', nombres: 'Ana', apellidos: 'P', correo: 'a@b.cl', telefono: '+56 9 1234 5678', cargo: '', rol: 'EMPLEADO', sucursalId: 1, equipoId: '', fechaIngresoRotacion: '2026-01-01' })
    expect(e).toEqual({})
  })
})
