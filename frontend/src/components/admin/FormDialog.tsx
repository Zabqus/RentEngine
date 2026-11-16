import type { FormEvent, ReactNode } from 'react'
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack } from '@mui/material'

interface Props {
  title: string
  pending: boolean
  error: string | null
  onSubmit: () => void
  onClose: () => void
  children: ReactNode
}

// Wspólna ramka formularzy admina; montowana tylko, gdy otwarta, więc stan formularza startuje od nowa
export function FormDialog({ title, pending, error, onSubmit, onClose, children }: Props) {
  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    onSubmit()
  }

  return (
    <Dialog open onClose={pending ? undefined : onClose} fullWidth maxWidth="sm">
      <form onSubmit={handleSubmit} noValidate>
        <DialogTitle>{title}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {error && <Alert severity="error">{error}</Alert>}
            {children}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={pending}>
            Anuluj
          </Button>
          <Button type="submit" variant="contained" loading={pending}>
            Zapisz
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
