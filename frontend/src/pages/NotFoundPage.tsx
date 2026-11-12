import { Button, Stack, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router'

export function NotFoundPage() {
  return (
    <Stack spacing={2} sx={{ alignItems: 'flex-start' }}>
      <Typography variant="h4">Nie znaleziono strony</Typography>
      <Button variant="contained" component={RouterLink} to="/">
        Strona główna
      </Button>
    </Stack>
  )
}
