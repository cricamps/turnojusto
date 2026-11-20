/** Configuración leída de las variables de Vite (se fijan al compilar en el pipeline). */
export const config = {
  clientId: import.meta.env.VITE_ENTRA_CLIENT_ID as string,
  authority: import.meta.env.VITE_ENTRA_AUTHORITY as string,
  apiScope: import.meta.env.VITE_API_SCOPE as string,
  apiUrl: (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, '') ?? '',
}
