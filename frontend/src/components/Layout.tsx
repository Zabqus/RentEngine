import { AppBar, Box, Button, Chip, Container, Stack, Toolbar, Typography } from '@mui/material'
import AgricultureIcon from '@mui/icons-material/Agriculture'
import LogoutIcon from '@mui/icons-material/Logout'
import { Link as RouterLink, Outlet, useNavigate } from 'react-router'
import type { Role } from '../api/auth'
import { ROLE_LABELS } from '../auth/roles'
import { useAuth } from '../auth/useAuth'

const NAV_LINKS: Record<Role, { to: string; label: string }[]> = {
  CLIENT: [{ to: '/my-rentals', label: 'Moje umowy' }],
  EMPLOYEE: [{ to: '/rentals', label: 'Umowy' }],
  ADMIN: [
    { to: '/rentals', label: 'Umowy' },
    { to: '/admin', label: 'Administracja' },
  ],
}

export function Layout() {
  const { user, role, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <AppBar position="static">
        <Toolbar sx={{ gap: 2, flexWrap: 'wrap' }}>
          <Stack direction="row" component={RouterLink} to="/" sx={{ alignItems: 'center', color: 'inherit', textDecoration: 'none' }}>
            <AgricultureIcon sx={{ mr: 1 }} />
            <Typography variant="h6" sx={{ fontWeight: 700 }}>
              RentEngine
            </Typography>
          </Stack>

          <Stack direction="row" spacing={1} sx={{ flex: 1 }}>
            {role &&
              NAV_LINKS[role].map((link) => (
                <Button key={link.to} color="inherit" component={RouterLink} to={link.to}>
                  {link.label}
                </Button>
              ))}
          </Stack>

          {user ? (
            <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
              <Typography variant="body2" sx={{ display: { xs: 'none', sm: 'block' } }}>
                {user.firstName} {user.lastName}
              </Typography>
              <Chip label={ROLE_LABELS[user.role]} size="small" sx={{ bgcolor: 'rgba(255,255,255,0.2)', color: 'inherit' }} />
              <Button color="inherit" startIcon={<LogoutIcon />} onClick={handleLogout}>
                Wyloguj
              </Button>
            </Stack>
          ) : (
            <Stack direction="row" spacing={1}>
              <Button color="inherit" component={RouterLink} to="/login">
                Zaloguj
              </Button>
              <Button variant="outlined" color="inherit" component={RouterLink} to="/register">
                Załóż konto
              </Button>
            </Stack>
          )}
        </Toolbar>
      </AppBar>
      <Container component="main" sx={{ py: 4, flex: 1 }}>
        <Outlet />
      </Container>
    </Box>
  )
}
