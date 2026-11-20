/**
 * Configuración de la SPA. Se toma de /config.json al cargar la página (así un mismo build
 * sirve para CloudFront y para turnojusto.xkcx.cl) y, si no existe, de las variables de Vite.
 */
export const config = {
  clientId: import.meta.env.VITE_ENTRA_CLIENT_ID as string,
  authority: import.meta.env.VITE_ENTRA_AUTHORITY as string,
  apiScope: import.meta.env.VITE_API_SCOPE as string,
  apiUrl: (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, '') ?? '',
}

export async function cargarConfig(): Promise<void> {
  try {
    const r = await fetch('/config.json', { cache: 'no-store' })
    if (!r.ok) return
    const c = (await r.json()) as Partial<typeof config>
    Object.assign(config, Object.fromEntries(Object.entries(c).filter(([, v]) => typeof v === 'string' && v)))
    config.apiUrl = config.apiUrl.replace(/\/$/, '')
  } catch {
    // Sin config.json se usan los valores del build.
  }
}
