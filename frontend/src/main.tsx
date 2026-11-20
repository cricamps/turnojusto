import '@fontsource/inter/400.css'
import '@fontsource/inter/600.css'
import '@fontsource/inter/700.css'
import { EventType, type AuthenticationResult } from '@azure/msal-browser'
import { MsalProvider } from '@azure/msal-react'
import { CssBaseline, ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import { msal } from './auth/msal'
import { NotificadorProvider } from './componentes/Notificador'
import { tema } from './tema'

const consultas = new QueryClient({ defaultOptions: { queries: { staleTime: 30_000, refetchOnWindowFocus: false, retry: 1 } } })

msal.initialize().then(async () => {
  const resultado = await msal.handleRedirectPromise().catch(() => null)
  const cuenta = resultado?.account ?? msal.getAllAccounts()[0]
  if (cuenta) msal.setActiveAccount(cuenta)
  msal.addEventCallback((evento) => {
    if (evento.eventType === EventType.LOGIN_SUCCESS && evento.payload) {
      msal.setActiveAccount((evento.payload as AuthenticationResult).account)
    }
  })
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <MsalProvider instance={msal}>
        <QueryClientProvider client={consultas}>
          <ThemeProvider theme={tema}>
            <CssBaseline />
            <NotificadorProvider>
              <BrowserRouter><App /></BrowserRouter>
            </NotificadorProvider>
          </ThemeProvider>
        </QueryClientProvider>
      </MsalProvider>
    </StrictMode>,
  )
})
