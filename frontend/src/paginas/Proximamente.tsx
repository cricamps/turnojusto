import { Alert } from '@mui/material'
import { Encabezado } from '../componentes/Comunes'

export function Proximamente({ titulo, sprint }: { titulo: string; sprint: string }) {
  return (
    <>
      <Encabezado titulo={titulo} />
      <Alert severity="info">Esta funcionalidad se implementa en el {sprint}, según el plan de trabajo.</Alert>
    </>
  )
}
