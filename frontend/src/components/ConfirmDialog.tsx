import { Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle } from '@mui/material'

interface Props {
  open: boolean
  title: string
  message: string
  confirmLabel: string
  pending?: boolean
  onConfirm: () => void
  onClose: () => void
}

export function ConfirmDialog({ open, title, message, confirmLabel, pending, onConfirm, onClose }: Props) {
  return (
    <Dialog open={open} onClose={pending ? undefined : onClose}>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <DialogContentText>{message}</DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={pending}>
          Nie
        </Button>
        <Button color="error" variant="contained" onClick={onConfirm} loading={pending}>
          {confirmLabel}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
