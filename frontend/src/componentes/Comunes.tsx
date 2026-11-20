import { Avatar, Box, Chip, Paper, Stack, Typography } from '@mui/material'
import type { ReactNode } from 'react'
import { colorAvatar, iniciales } from '../util/formato'

/** Avatar con iniciales y color estable por persona. */
export function AvatarPersona({ nombre, tamano = 32 }: { nombre: string; tamano?: number }) {
  return (
    <Avatar sx={{ width: tamano, height: tamano, fontSize: tamano * 0.4, bgcolor: colorAvatar(nombre), fontWeight: 700 }}>
      {iniciales(nombre)}
    </Avatar>
  )
}

/** Estado con texto y color (nunca solo color, por accesibilidad). */
export function EstadoChip({ estado }: { estado: string }) {
  const activo = estado === 'ACTIVO' || estado === 'ACTIVA'
  return (
    <Chip size="small" label={activo ? (estado === 'ACTIVA' ? 'Activa' : 'Activo') : estado === 'INACTIVA' ? 'Inactiva' : 'Inactivo'}
      sx={{ bgcolor: activo ? '#dcfce7' : '#f1f5f9', color: activo ? '#15803d' : '#475569', height: 22 }} />
  )
}

/** Tarjeta de indicador de la pantalla de inicio. */
export function Kpi({ icono, titulo, valor, detalle }: { icono: ReactNode; titulo: string; valor: ReactNode; detalle?: string }) {
  return (
    <Paper sx={{ p: 2, flex: 1, minWidth: 180 }}>
      <Stack direction="row" spacing={1} alignItems="center" sx={{ color: 'text.secondary', mb: 1 }}>
        {icono}
        <Typography variant="body2">{titulo}</Typography>
      </Stack>
      <Typography variant="h4" component="p">{valor}</Typography>
      {detalle && <Typography variant="caption" color="text.secondary">{detalle}</Typography>}
    </Paper>
  )
}

/** Encabezado de página con título, subtítulo y acciones a la derecha. */
export function Encabezado({ titulo, subtitulo, acciones }: { titulo: string; subtitulo?: string; acciones?: ReactNode }) {
  return (
    <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} spacing={2} sx={{ mb: 3 }}>
      <Box>
        <Typography variant="h4" component="h1">{titulo}</Typography>
        {subtitulo && <Typography color="text.secondary">{subtitulo}</Typography>}
      </Box>
      {acciones && <Stack direction="row" spacing={1}>{acciones}</Stack>}
    </Stack>
  )
}

/** Estado vacío con mensaje y acción sugerida. */
export function Vacio({ texto, accion }: { texto: string; accion?: ReactNode }) {
  return (
    <Box sx={{ py: 6, textAlign: 'center', color: 'text.secondary' }}>
      <Typography sx={{ mb: 2 }}>{texto}</Typography>
      {accion}
    </Box>
  )
}
