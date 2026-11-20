import { InteractionRequiredAuthError } from '@azure/msal-browser'
import { apiRequest, msal } from '../auth/msal'
import { config } from '../config'

/** Error de la API con el mensaje en español (RFC 9457) y los errores por campo. */
export class ErrorApi extends Error {
  readonly estado: number
  readonly campos: Record<string, string>

  constructor(estado: number, mensaje: string, campos: Record<string, string> = {}) {
    super(mensaje)
    this.estado = estado
    this.campos = campos
  }
}

async function token(): Promise<string> {
  const cuenta = msal.getActiveAccount() ?? msal.getAllAccounts()[0]
  if (!cuenta) throw new ErrorApi(401, 'Tu sesión expiró. Vuelve a iniciar sesión.')
  try {
    const r = await msal.acquireTokenSilent({ ...apiRequest, account: cuenta })
    return r.accessToken
  } catch (e) {
    if (e instanceof InteractionRequiredAuthError) {
      await msal.acquireTokenRedirect({ ...apiRequest, account: cuenta })
    }
    throw e
  }
}

/** Llama a la API a través de Amazon API Gateway con el token de Entra External ID. */
export async function api<T>(ruta: string, opciones: { metodo?: string; cuerpo?: unknown } = {}): Promise<T> {
  const respuesta = await fetch(`${config.apiUrl}${ruta}`, {
    method: opciones.metodo ?? 'GET',
    headers: {
      Authorization: `Bearer ${await token()}`,
      ...(opciones.cuerpo !== undefined ? { 'Content-Type': 'application/json' } : {}),
    },
    body: opciones.cuerpo !== undefined ? JSON.stringify(opciones.cuerpo) : undefined,
  })
  if (!respuesta.ok) {
    let detalle = 'Ocurrió un error inesperado. Intenta nuevamente.'
    let campos: Record<string, string> = {}
    try {
      const p = await respuesta.json()
      detalle = p.detail ?? p.message ?? detalle
      campos = p.campos ?? {}
    } catch {
      if (respuesta.status === 401) detalle = 'Tu sesión expiró. Vuelve a iniciar sesión.'
      if (respuesta.status >= 500) detalle = 'El servicio no está disponible en este momento.'
    }
    throw new ErrorApi(respuesta.status, detalle, campos)
  }
  if (respuesta.status === 204) return undefined as T
  return respuesta.json() as Promise<T>
}
