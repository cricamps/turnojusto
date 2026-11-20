import CloseIcon from '@mui/icons-material/Close'
import { Alert, Box, Button, Drawer, IconButton, Stack, Typography } from '@mui/material'
import type { FormEvent, ReactNode } from 'react'

/**
 * Formulario en panel lateral (patrón drawer de los mockups): no se pierde el contexto de la
 * lista, los botones quedan fijos abajo y Esc cierra el panel.
 */
export function PanelLateral({ abierto, titulo, subtitulo, error, guardando, textoGuardar, alCerrar, alGuardar, children }: {
  abierto: boolean
  titulo: string
  subtitulo?: string
  error?: string | null
  guardando?: boolean
  textoGuardar: string
  alCerrar: () => void
  alGuardar: () => void
  children: ReactNode
}) {
  const enviar = (e: FormEvent) => {
    e.preventDefault()
    alGuardar()
  }
  return (
    <Drawer anchor="right" open={abierto} onClose={alCerrar} slotProps={{ paper: { sx: { width: { xs: '100%', sm: 480 } } } }}>
      <Box component="form" onSubmit={enviar} noValidate sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
        <Stack direction="row" justifyContent="space-between" sx={{ p: 3, pb: 1 }}>
          <Box>
            <Typography variant="h6" component="h2">{titulo}</Typography>
            <Typography variant="body2" color="text.secondary">{subtitulo ?? 'Los campos con * son obligatorios'}</Typography>
          </Box>
          <IconButton onClick={alCerrar} aria-label="Cerrar"><CloseIcon /></IconButton>
        </Stack>
        <Box sx={{ flex: 1, overflowY: 'auto', p: 3, pt: 2 }}>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <Stack spacing={2}>{children}</Stack>
        </Box>
        <Stack direction="row" justifyContent="flex-end" spacing={1} sx={{ p: 2, borderTop: '1px solid #e2e8f0' }}>
          <Button onClick={alCerrar}>Cancelar</Button>
          <Button type="submit" variant="contained" disabled={guardando}>{guardando ? 'Guardando…' : textoGuardar}</Button>
        </Stack>
      </Box>
    </Drawer>
  )
}
