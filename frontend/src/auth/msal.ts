import { PublicClientApplication, type Configuration } from '@azure/msal-browser'
import { config } from '../config'

/**
 * Inicio de sesión con Microsoft Entra External ID (OIDC + PKCE). TurnoJusto no guarda
 * contraseñas: la identidad, el MFA y la recuperación de cuenta los resuelve Entra.
 */
const host = (() => {
  try {
    return new URL(config.authority).host
  } catch {
    return 'login.microsoftonline.com'
  }
})()

const msalConfig: Configuration = {
  auth: {
    clientId: config.clientId,
    authority: config.authority,
    knownAuthorities: [host],
    redirectUri: window.location.origin,
    postLogoutRedirectUri: window.location.origin,
  },
  cache: { cacheLocation: 'sessionStorage' },
}

export const msal = new PublicClientApplication(msalConfig)

/** Alcances que se piden al iniciar sesión y al llamar a la API. */
export const loginRequest = { scopes: ['openid', 'profile', 'email', config.apiScope] }
export const apiRequest = { scopes: [config.apiScope] }
