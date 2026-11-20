import { Alert, Box, Button, Paper, Stack, Typography } from '@mui/material'
import { useMsal } from '@azure/msal-react'

/** La cuenta existe en Entra, pero no hay una ficha activa de empleado con ese correo. */
export function SinAcceso({ mensaje }: { mensaje: string }) {
  const { instance, accounts } = useMsal()
  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', p: 3 }}>
      <Paper sx={{ p: 4, maxWidth: 520 }}>
        <Stack spacing={2}>
          <Typography variant="h5">No tienes acceso todavía</Typography>
          <Alert severity="warning">{mensaje}</Alert>
          <Typography variant="body2" color="text.secondary">Cuenta: {accounts[0]?.username}</Typography>
          <Button variant="outlined" onClick={() => instance.logoutRedirect()}>Cerrar sesión</Button>
        </Stack>
      </Paper>
    </Box>
  )
}
