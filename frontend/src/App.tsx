import { Box, CircularProgress } from '@mui/material'
import { AuthenticatedTemplate, UnauthenticatedTemplate, useIsAuthenticated } from '@azure/msal-react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { ErrorApi } from './api/cliente'
import { usePerfil } from './api/hooks'
import { Layout } from './componentes/Layout'
import { EmpleadoDetalle } from './paginas/EmpleadoDetalle'
import { Empleados } from './paginas/Empleados'
import { Empresa } from './paginas/Empresa'
import { Equipos } from './paginas/Equipos'
import { Inicio } from './paginas/Inicio'
import { Login } from './paginas/Login'
import { Proximamente } from './paginas/Proximamente'
import { SinAcceso } from './paginas/SinAcceso'

function AppAutenticada() {
  const autenticado = useIsAuthenticated()
  const { data: perfil, error, isLoading } = usePerfil(autenticado)
  if (isLoading || (!perfil && !error)) {
    return <Box sx={{ display: 'grid', placeItems: 'center', minHeight: '100vh' }}><CircularProgress /></Box>
  }
  if (error || !perfil) {
    const mensaje = error instanceof ErrorApi && error.estado === 403 ? error.message
      : `No se pudo cargar tu perfil: ${error?.message ?? 'error desconocido'}`
    return <SinAcceso mensaje={mensaje} />
  }
  const esEmpleado = perfil.rol === 'EMPLEADO'
  return (
    <Routes>
      <Route element={<Layout perfil={perfil} />}>
        <Route index element={esEmpleado ? <Navigate to="/mi-ficha" replace /> : <Inicio perfil={perfil} />} />
        <Route path="mi-ficha" element={<EmpleadoDetalle perfil={perfil} idPropio={perfil.empleadoId} />} />
        {perfil.rol === 'ADMINISTRADOR' && <Route path="empresa" element={<Empresa />} />}
        {!esEmpleado && <Route path="equipos" element={<Equipos perfil={perfil} />} />}
        {!esEmpleado && <Route path="empleados" element={<Empleados perfil={perfil} />} />}
        {!esEmpleado && <Route path="empleados/:id" element={<EmpleadoDetalle perfil={perfil} />} />}
        <Route path="turnos" element={<Proximamente titulo="Configuración de turnos" sprint="Sprint 2 (semana 6)" />} />
        <Route path="calendario" element={<Proximamente titulo="Calendario y propuesta" sprint="Sprint 2 (semana 6)" />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}

export default function App() {
  return (
    <>
      <AuthenticatedTemplate><AppAutenticada /></AuthenticatedTemplate>
      <UnauthenticatedTemplate><Login /></UnauthenticatedTemplate>
    </>
  )
}
