import { useCallback, useMemo, useState, type ReactNode } from 'react'
import { Alert, Snackbar } from '@mui/material'
import { getApiError } from '../api/errors'
import { NotificationContext, type Notify } from './context'

interface Toast {
  key: number
  severity: 'success' | 'error'
  message: string
}

export function NotificationProvider({ children }: { children: ReactNode }) {
  const [toast, setToast] = useState<Toast | null>(null)
  const [open, setOpen] = useState(false)

  const show = useCallback((severity: Toast['severity'], message: string) => {
    setToast({ key: Date.now(), severity, message })
    setOpen(true)
  }, [])

  const notify = useMemo<Notify>(
    () => ({
      success: (message) => show('success', message),
      error: (message) => show('error', message),
      apiError: (error) => show('error', getApiError(error).message),
    }),
    [show],
  )

  return (
    <NotificationContext.Provider value={notify}>
      {children}
      <Snackbar
        key={toast?.key}
        open={open}
        autoHideDuration={toast?.severity === 'error' ? 8000 : 4000}
        onClose={(_, reason) => reason !== 'clickaway' && setOpen(false)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity={toast?.severity} variant="filled" onClose={() => setOpen(false)} sx={{ width: '100%' }}>
          {toast?.message}
        </Alert>
      </Snackbar>
    </NotificationContext.Provider>
  )
}
