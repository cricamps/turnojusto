import { createTheme } from '@mui/material/styles'
import { esES } from '@mui/material/locale'

/** Paleta y tipografía de los mockups de la semana 4 (verde azulado sobre barra lateral oscura). */
export const tema = createTheme(
  {
    palette: {
      primary: { main: '#0f766e', dark: '#115e59', light: '#ccfbf1', contrastText: '#ffffff' },
      secondary: { main: '#1e293b' },
      error: { main: '#dc2626' },
      warning: { main: '#d97706' },
      success: { main: '#16a34a' },
      background: { default: '#f4f6f8', paper: '#ffffff' },
      text: { primary: '#0f172a', secondary: '#64748b' },
      divider: '#e2e8f0',
    },
    shape: { borderRadius: 8 },
    typography: {
      fontFamily: '"Inter", system-ui, -apple-system, "Segoe UI", sans-serif',
      h4: { fontWeight: 700, fontSize: '1.6rem' },
      h5: { fontWeight: 700, fontSize: '1.3rem' },
      h6: { fontWeight: 600, fontSize: '1.05rem' },
      button: { textTransform: 'none', fontWeight: 600 },
    },
    components: {
      MuiPaper: { defaultProps: { elevation: 0 }, styleOverrides: { root: { border: '1px solid #e2e8f0' } } },
      MuiAppBar: { styleOverrides: { root: { border: 'none' } } },
      MuiDrawer: { styleOverrides: { paper: { border: 'none' } } },
      MuiTableCell: {
        styleOverrides: {
          head: { fontSize: 12, fontWeight: 600, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.3 },
        },
      },
      MuiButton: { defaultProps: { disableElevation: true } },
      MuiTextField: { defaultProps: { size: 'small', fullWidth: true } },
      MuiSelect: { defaultProps: { size: 'small' } },
      MuiChip: { styleOverrides: { root: { fontWeight: 600 } } },
    },
  },
  esES,
)
