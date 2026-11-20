import LockIcon from '@mui/icons-material/LockOutlined'
import { Box, Button, Paper, Stack, Typography } from '@mui/material'
import { useMsal } from '@azure/msal-react'
import { loginRequest } from '../auth/msal'

/** P01 · Inicio de sesión. La autenticación (correo, contraseña y MFA) ocurre en Entra External ID. */
export function Login() {
  const { instance } = useMsal()
  return (
    <Box sx={{ minHeight: '100vh', display: 'grid', gridTemplateColumns: { md: '1.1fr 1fr' }, bgcolor: '#0f2f33' }}>
      <Box sx={{ display: { xs: 'none', md: 'flex' }, flexDirection: 'column', justifyContent: 'center', p: 8, color: '#e2f3f1' }}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 4 }}>
          <Box component="img" src="/favicon.svg" alt="" sx={{ width: 44, height: 44 }} />
          <Typography variant="h4" sx={{ color: '#fff' }}>TurnoJusto</Typography>
        </Stack>
        <Typography variant="h5" sx={{ color: '#fff', maxWidth: 480, mb: 2 }}>
          Turnos rotativos equitativos para empresas con sucursales.
        </Typography>
        <Typography sx={{ maxWidth: 480, color: '#9fb6ba' }}>
          Registra tus sucursales, equipos y empleados, y genera calendarios donde cada persona cubre los turnos que le
          corresponden según su disponibilidad.
        </Typography>
      </Box>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', bgcolor: '#f4f6f8', p: 3 }}>
        <Paper sx={{ p: 5, width: '100%', maxWidth: 420 }}>
          <Stack spacing={3}>
            <Box>
              <Typography variant="h5" component="h1">Iniciar sesión</Typography>
              <Typography color="text.secondary">Usa el correo con el que tu empresa te registró.</Typography>
            </Box>
            <Button variant="contained" size="large" startIcon={<LockIcon />}
              onClick={() => instance.loginRedirect(loginRequest)}>
              Ingresar con mi cuenta
            </Button>
            <Typography variant="caption" color="text.secondary">
              El acceso se valida con Microsoft Entra External ID. TurnoJusto no guarda tu contraseña. Si no tienes cuenta,
              pide al administrador de tu empresa que te registre; luego crea tu cuenta con ese mismo correo.
            </Typography>
          </Stack>
        </Paper>
      </Box>
    </Box>
  )
}
