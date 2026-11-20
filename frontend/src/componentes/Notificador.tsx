import { Alert, Snackbar } from '@mui/material'
import { createContext, useCallback, useContext, useState, type ReactNode } from 'react'

type Tipo = 'success' | 'error' | 'info'
const Ctx = createContext<(mensaje: string, tipo?: Tipo) => void>(() => {})

/** Mensajes breves de confirmación o error (aparecen abajo a la izquierda). */
export function NotificadorProvider({ children }: { children: ReactNode }) {
  const [aviso, setAviso] = useState<{ mensaje: string; tipo: Tipo } | null>(null)
  const avisar = useCallback((mensaje: string, tipo: Tipo = 'success') => setAviso({ mensaje, tipo }), [])
  return (
    <Ctx.Provider value={avisar}>
      {children}
      <Snackbar open={!!aviso} autoHideDuration={5000} onClose={() => setAviso(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}>
        <Alert severity={aviso?.tipo ?? 'info'} variant="filled" onClose={() => setAviso(null)} sx={{ minWidth: 320 }}>
          {aviso?.mensaje}
        </Alert>
      </Snackbar>
    </Ctx.Provider>
  )
}

export const useAvisar = () => useContext(Ctx)
