import { PublicClientApplication } from '@azure/msal-browser'
import { config } from '../config'

/**
 * Inicio de sesión con Microsoft Entra (OIDC + PKCE). TurnoJusto no guarda contraseñas:
 * la identidad, el MFA y la recuperación de cuenta los resuelve Entra.
 */
let instancia: PublicClientApplication | null = null

/** Crea la instancia de MSAL con la configuración ya cargada (se llama una vez en main.tsx). */
export function crearMsal(): PublicClientApplication {
  instancia = new PublicClientApplication({
    auth: {
      clientId: config.clientId,
      authority: config.authority,
      redirectUri: window.location.origin,
      postLogoutRedirectUri: window.location.origin,
    },
    cache: { cacheLocation: 'sessionStorage' },
  })
  return instancia
}

export function msal(): PublicClientApplication {
  if (!instancia) throw new Error('MSAL no está inicializado')
  return instancia
}

/** Alcances que se piden al iniciar sesión y al llamar a la API. */
export const loginRequest = () => ({ scopes: ['openid', 'profile', 'email', config.apiScope] })
export const apiRequest = () => ({ scopes: [config.apiScope] })
