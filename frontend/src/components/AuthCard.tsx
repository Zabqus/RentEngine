import type { FormEvent, ReactNode } from 'react'
import { Alert, Box, Button, Card, CardContent, Stack, Typography } from '@mui/material'

interface Props {
  title: string
  submitLabel: string
  pending: boolean
  error: string | null
  onSubmit: () => void
  footer: ReactNode
  children: ReactNode
}

// Wspólna ramka formularzy logowania i rejestracji
export function AuthCard({ title, submitLabel, pending, error, onSubmit, footer, children }: Props) {
  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    onSubmit()
  }

  return (
    <Box sx={{ display: 'flex', justifyContent: 'center' }}>
      <Card sx={{ width: '100%', maxWidth: 420 }}>
        <CardContent sx={{ p: 4 }}>
          <Typography variant="h5" gutterBottom>
            {title}
          </Typography>
          <Stack component="form" spacing={2} noValidate onSubmit={handleSubmit}>
            {error && <Alert severity="error">{error}</Alert>}
            {children}
            <Button type="submit" variant="contained" size="large" loading={pending}>
              {submitLabel}
            </Button>
            <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center' }}>
              {footer}
            </Typography>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  )
}
