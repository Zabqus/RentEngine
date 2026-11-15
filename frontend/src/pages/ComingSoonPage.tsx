import { Alert, Stack, Typography } from '@mui/material'

// Tymczasowa strona dla tras ról, do zastąpienia w Dniu 10
export function ComingSoonPage({ title }: { title: string }) {
  return (
    <Stack spacing={2}>
      <Typography variant="h4">{title}</Typography>
      <Alert severity="info">Ta sekcja jest w przygotowaniu.</Alert>
    </Stack>
  )
}
