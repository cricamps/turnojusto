import BarChartIcon from '@mui/icons-material/BarChartOutlined'
import BadgeIcon from '@mui/icons-material/BadgeOutlined'
import CalendarIcon from '@mui/icons-material/CalendarMonthOutlined'
import ApartmentIcon from '@mui/icons-material/ApartmentOutlined'
import GroupsIcon from '@mui/icons-material/GroupsOutlined'
import HomeIcon from '@mui/icons-material/HomeOutlined'
import LogoutIcon from '@mui/icons-material/Logout'
import MenuIcon from '@mui/icons-material/Menu'
import ScheduleIcon from '@mui/icons-material/ScheduleOutlined'
import ShieldIcon from '@mui/icons-material/VerifiedUserOutlined'
import SwapIcon from '@mui/icons-material/SwapHorizOutlined'
import {
  AppBar, Box, Chip, Drawer, IconButton, List, ListItemButton, ListItemIcon, ListItemText, ListSubheader, Menu,
  MenuItem, Stack, Toolbar, Tooltip, Typography, useMediaQuery,
} from '@mui/material'
import { useMsal } from '@azure/msal-react'
import { useState, type ReactNode } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import type { Perfil } from '../api/tipos'
import { useSucursales } from '../api/hooks'
import { nombreRol } from '../util/formato'
import { AvatarPersona } from './Comunes'

const ANCHO = 232

interface Item {
  texto: string
  ruta: string
  icono: ReactNode
  roles: Perfil['rol'][]
  sprint?: string
}

/** Sprint en que se entrega cada opción que todavía no está disponible. */
const cuando = (sprint: string) => (sprint === 'S2' ? 'Sprint 2 (semana 6)' : 'Sprint 3 (semana 7)')

/** Menú por rol. Lo que llega en los sprints 2 y 3 se muestra deshabilitado, como «Próximamente», con un aviso del sprint. */
const secciones: { titulo: string; items: Item[] }[] = [
  {
    titulo: 'Principal',
    items: [
      { texto: 'Inicio', ruta: '/', icono: <HomeIcon />, roles: ['ADMINISTRADOR', 'COORDINADOR'] },
      { texto: 'Mi ficha', ruta: '/mi-ficha', icono: <BadgeIcon />, roles: ['EMPLEADO'] },
      { texto: 'Empresa y sucursales', ruta: '/empresa', icono: <ApartmentIcon />, roles: ['ADMINISTRADOR'] },
      { texto: 'Equipos', ruta: '/equipos', icono: <GroupsIcon />, roles: ['ADMINISTRADOR', 'COORDINADOR'] },
      { texto: 'Empleados', ruta: '/empleados', icono: <BadgeIcon />, roles: ['ADMINISTRADOR', 'COORDINADOR'] },
    ],
  },
  {
    titulo: 'Turnos',
    items: [
      { texto: 'Configuración de turnos', ruta: '/turnos', icono: <ScheduleIcon />, roles: ['ADMINISTRADOR', 'COORDINADOR'], sprint: 'S2' },
      { texto: 'Calendario y propuesta', ruta: '/calendario', icono: <CalendarIcon />, roles: ['ADMINISTRADOR', 'COORDINADOR', 'EMPLEADO'], sprint: 'S2' },
      { texto: 'Reemplazos', ruta: '/reemplazos', icono: <SwapIcon />, roles: ['ADMINISTRADOR', 'COORDINADOR', 'EMPLEADO'], sprint: 'S3' },
    ],
  },
  {
    titulo: 'Análisis',
    items: [{ texto: 'Reportes', ruta: '/reportes', icono: <BarChartIcon />, roles: ['ADMINISTRADOR', 'COORDINADOR'], sprint: 'S3' }],
  },
]

const titulos: Record<string, string> = {
  '': 'Inicio', empresa: 'Empresa y sucursales', equipos: 'Equipos', empleados: 'Empleados', 'mi-ficha': 'Mi ficha',
  turnos: 'Configuración de turnos', calendario: 'Calendario y propuesta', reemplazos: 'Reemplazos', reportes: 'Reportes',
}

function Menulateral({ perfil, alNavegar }: { perfil: Perfil; alNavegar: () => void }) {
  return (
    <Box sx={{ height: '100%', bgcolor: '#0f2f33', color: '#cbd5e1', display: 'flex', flexDirection: 'column' }}>
      <Stack direction="row" spacing={1.2} alignItems="center" sx={{ px: 2.5, py: 2.2 }}>
        <Box component="img" src="/favicon.svg" alt="" sx={{ width: 30, height: 30 }} />
        <Typography sx={{ color: '#fff', fontWeight: 700, fontSize: 18 }}>TurnoJusto</Typography>
      </Stack>
      <Box sx={{ flex: 1, overflowY: 'auto' }}>
        {secciones.map((s) => {
          const items = s.items.filter((i) => i.roles.includes(perfil.rol))
          if (!items.length) return null
          return (
            <List key={s.titulo} dense
              subheader={<ListSubheader sx={{ bgcolor: 'transparent', color: '#7d9aa0', fontSize: 11, textTransform: 'uppercase', letterSpacing: 0.6, lineHeight: '32px' }}>{s.titulo}</ListSubheader>}>
              {items.map((i) => {
                const boton = (
                  <ListItemButton key={i.ruta} component={i.sprint ? 'div' : NavLink} to={i.sprint ? undefined : i.ruta}
                    end={i.ruta === '/'} disabled={!!i.sprint} onClick={alNavegar}
                    aria-label={i.sprint ? `${i.texto}: próximamente, ${cuando(i.sprint)}` : undefined}
                    sx={{
                      mx: 1, borderRadius: 1.5, color: '#cbd5e1', '& .MuiListItemIcon-root': { color: '#9fb6ba', minWidth: 34 },
                      '&.active': { bgcolor: '#155e63', color: '#fff', '& .MuiListItemIcon-root': { color: '#fff' } },
                      '&.Mui-disabled': { opacity: 0.6 },
                    }}>
                    <ListItemIcon>{i.icono}</ListItemIcon>
                    <ListItemText primary={i.texto}
                      secondary={i.sprint ? 'Próximamente' : undefined}
                      slotProps={{ primary: { fontSize: 14 }, secondary: { fontSize: 11, color: '#7fc4bd' } }} />
                  </ListItemButton>
                )
                if (!i.sprint) return boton
                // Un elemento deshabilitado no recibe eventos del mouse: el span permite mostrar el aviso.
                return (
                  <Tooltip key={i.ruta} title={`Disponible en el ${cuando(i.sprint)}`} placement="right" arrow>
                    <Box component="span" sx={{ display: 'block', cursor: 'not-allowed' }} tabIndex={0}>{boton}</Box>
                  </Tooltip>
                )
              })}
            </List>
          )
        })}
      </Box>
      <Stack direction="row" spacing={1} alignItems="center" sx={{ px: 2.5, py: 2, fontSize: 12, color: '#7d9aa0' }}>
        <ShieldIcon fontSize="small" />
        <span>Sesión protegida con Microsoft Entra ID</span>
      </Stack>
    </Box>
  )
}

/** Estructura común: barra lateral por rol, barra superior con ruta, sucursal y usuario. */
export function Layout({ perfil }: { perfil: Perfil }) {
  const { instance } = useMsal()
  const movil = useMediaQuery('(max-width:900px)')
  const [abierto, setAbierto] = useState(false)
  const [menu, setMenu] = useState<HTMLElement | null>(null)
  const { pathname } = useLocation()
  const { data: sucursales } = useSucursales()
  const seccion = pathname.split('/')[1] ?? ''
  const miSucursal = sucursales?.find((s) => s.id === perfil.sucursalId)

  const lateral = <Menulateral perfil={perfil} alNavegar={() => setAbierto(false)} />
  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      {movil ? (
        <Drawer open={abierto} onClose={() => setAbierto(false)} slotProps={{ paper: { sx: { width: ANCHO } } }}>{lateral}</Drawer>
      ) : (
        <Box component="nav" sx={{ width: ANCHO, flexShrink: 0, position: 'fixed', inset: '0 auto 0 0' }}>{lateral}</Box>
      )}
      <Box sx={{ flex: 1, ml: movil ? 0 : `${ANCHO}px`, minWidth: 0 }}>
        <AppBar position="sticky" color="inherit" sx={{ borderBottom: '1px solid #e2e8f0', bgcolor: '#fff' }}>
          <Toolbar sx={{ gap: 2 }}>
            {movil && <IconButton onClick={() => setAbierto(true)} aria-label="Abrir menú"><MenuIcon /></IconButton>}
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              <Box component="span" sx={{ color: 'text.secondary', fontWeight: 400 }}>{perfil.rol === 'ADMINISTRADOR' ? 'Empresa' : 'Mi equipo'} / </Box>
              {titulos[seccion] ?? ''}
            </Typography>
            <Chip size="small" variant="outlined" sx={{ display: { xs: 'none', sm: 'flex' } }}
              label={perfil.rol === 'ADMINISTRADOR' ? 'Todas las sucursales' : miSucursal?.nombre ?? 'Mi sucursal'} />
            <Box sx={{ flex: 1 }} />
            <Stack direction="row" spacing={1} alignItems="center" sx={{ cursor: 'pointer' }} onClick={(e) => setMenu(e.currentTarget)}>
              <AvatarPersona nombre={perfil.nombre} />
              <Box sx={{ display: { xs: 'none', sm: 'block' }, lineHeight: 1.1 }}>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>{perfil.nombre}</Typography>
                <Typography variant="caption" color="primary">{nombreRol[perfil.rol]}</Typography>
              </Box>
            </Stack>
            <Menu anchorEl={menu} open={!!menu} onClose={() => setMenu(null)}>
              <MenuItem disabled>{perfil.correo}</MenuItem>
              <MenuItem onClick={() => instance.logoutRedirect()}><LogoutIcon fontSize="small" sx={{ mr: 1 }} />Cerrar sesión</MenuItem>
            </Menu>
          </Toolbar>
        </AppBar>
        <Box component="main" sx={{ p: { xs: 2, md: 4 }, maxWidth: 1400 }}>
          <Outlet />
        </Box>
      </Box>
    </Box>
  )
}
