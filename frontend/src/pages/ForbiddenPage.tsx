import { Button, Stack, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router'

export function ForbiddenPage() {
  return (
    <Stack spacing={2} sx={{ alignItems: 'flex-start' }}>
      <Typography variant="h4">Brak dostępu</Typography>
      <Typography color="text.secondary">Ta strona jest dostępna tylko dla innej roli.</Typography>
      <Button variant="contained" component={RouterLink} to="/">
        Strona główna
      </Button>
    </Stack>
  )
}
