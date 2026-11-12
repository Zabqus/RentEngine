import { AppBar, Box, Container, Toolbar, Typography } from '@mui/material'
import AgricultureIcon from '@mui/icons-material/Agriculture'
import { Link as RouterLink, Outlet } from 'react-router'

export function Layout() {
  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <AppBar position="static">
        <Toolbar>
          <AgricultureIcon sx={{ mr: 1 }} />
          <Typography
            variant="h6"
            component={RouterLink}
            to="/"
            sx={{ color: 'inherit', textDecoration: 'none', fontWeight: 700 }}
          >
            RentEngine
          </Typography>
        </Toolbar>
      </AppBar>
      <Container component="main" sx={{ py: 4, flex: 1 }}>
        <Outlet />
      </Container>
    </Box>
  )
}
