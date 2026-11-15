import { useState, type FormEvent } from 'react'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
} from '@mui/material'
import { useMutation } from '@tanstack/react-query'
import { getApiError } from '../api/errors'
import { addDamage, type DamageRequest, type DamageSeverity, type Protocol } from '../api/protocols'
import { useNotify } from '../notifications/useNotify'

interface Props {
  protocol: Protocol | null
  onClose: () => void
  onSaved: () => void
}

type Field = 'description' | 'severity' | 'cost'

export function DamageDialog({ protocol, onClose, onSaved }: Props) {
  const notify = useNotify()
  const [description, setDescription] = useState('')
  const [severity, setSeverity] = useState<DamageSeverity>('MINOR')
  const [cost, setCost] = useState('0')
  const [errors, setErrors] = useState<Partial<Record<Field, string>>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const isIssue = protocol?.type === 'ISSUE'

  const reset = () => {
    setDescription('')
    setSeverity('MINOR')
    setCost('0')
    setErrors({})
    setFormError(null)
  }

  const save = useMutation({
    mutationFn: (request: DamageRequest) => addDamage(protocol!.id, request),
    onSuccess: () => {
      notify.success(severity === 'MAJOR' ? 'Uszkodzenie zapisane, egzemplarz trafił do serwisu.' : 'Uszkodzenie zapisane.')
      reset()
      onSaved()
    },
    onError: (error) => {
      const apiError = getApiError(error)
      setErrors(apiError.fieldErrors)
      setFormError(apiError.message)
    },
  })

  const handleClose = () => {
    reset()
    onClose()
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    const validation: Partial<Record<Field, string>> = {}
    if (!description.trim()) validation.description = 'Opisz uszkodzenie.'
    if (cost === '' || Number(cost) < 0) validation.cost = 'Podaj koszt (0 lub więcej).'
    setErrors(validation)
    setFormError(null)
    if (Object.keys(validation).length === 0) {
      save.mutate({ description: description.trim(), severity, cost: Number(cost) })
    }
  }

  return (
    <Dialog open={protocol !== null} onClose={save.isPending ? undefined : handleClose} fullWidth maxWidth="xs">
      <form onSubmit={handleSubmit} noValidate>
        <DialogTitle>Nowe uszkodzenie</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {formError && <Alert severity="error">{formError}</Alert>}
            {isIssue && (
              <Alert severity="info">Uszkodzenia przy wydaniu tylko dokumentują stan i nie obciążają klienta.</Alert>
            )}
            <TextField
              label="Opis"
              multiline
              minRows={2}
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              error={Boolean(errors.description)}
              helperText={errors.description}
              slotProps={{ htmlInput: { maxLength: 500 } }}
            />
            <TextField
              select
              label="Waga"
              value={severity}
              onChange={(event) => setSeverity(event.target.value as DamageSeverity)}
              error={Boolean(errors.severity)}
              helperText={errors.severity ?? (severity === 'MAJOR' ? 'Egzemplarz zostanie wysłany do serwisu.' : undefined)}
            >
              <MenuItem value="MINOR">Drobne</MenuItem>
              <MenuItem value="MAJOR" disabled={isIssue}>
                Poważne (serwis)
              </MenuItem>
            </TextField>
            <TextField
              label="Koszt naprawy (zł)"
              type="number"
              value={cost}
              onChange={(event) => setCost(event.target.value)}
              error={Boolean(errors.cost)}
              helperText={errors.cost}
              slotProps={{ htmlInput: { min: 0, step: 0.01 } }}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleClose} disabled={save.isPending}>
            Anuluj
          </Button>
          <Button type="submit" variant="contained" loading={save.isPending}>
            Zapisz
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
