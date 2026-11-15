import { useState, type FormEvent } from 'react'
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack, TextField } from '@mui/material'
import { useMutation } from '@tanstack/react-query'
import type { CategoryType } from '../api/catalog'
import { getApiError } from '../api/errors'
import { createProtocol, type ProtocolRequest, type ProtocolType } from '../api/protocols'
import { useNotify } from '../notifications/useNotify'
import { counterLabel, formatNumber } from '../utils/format'

interface Props {
  open: boolean
  rentalId: number
  type: ProtocolType
  tankCapacity: number | null
  categoryType: CategoryType
  currentReading: number
  onClose: () => void
  onSaved: () => void
}

type Field = 'fuelLevel' | 'mileageOrHours' | 'notes'

export function ProtocolDialog({ open, rentalId, type, tankCapacity, categoryType, currentReading, onClose, onSaved }: Props) {
  const notify = useNotify()
  const hasTank = tankCapacity !== null
  const [fuel, setFuel] = useState(hasTank ? String(tankCapacity) : '')
  const [reading, setReading] = useState(String(currentReading))
  const [notes, setNotes] = useState('')
  const [errors, setErrors] = useState<Partial<Record<Field, string>>>({})
  const [formError, setFormError] = useState<string | null>(null)

  const save = useMutation({
    mutationFn: (request: ProtocolRequest) => createProtocol(rentalId, request),
    onSuccess: () => {
      notify.success(type === 'ISSUE' ? 'Sprzęt wydany.' : 'Zwrot przyjęty.')
      onSaved()
    },
    onError: (error) => {
      const apiError = getApiError(error)
      setErrors(apiError.fieldErrors)
      setFormError(apiError.message)
    },
  })

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    const validation: Partial<Record<Field, string>> = {}
    const fuelValue = Number(fuel)
    if (hasTank && (fuel === '' || fuelValue < 0 || fuelValue > tankCapacity)) {
      validation.fuelLevel = `Podaj wartość od 0 do ${formatNumber(tankCapacity)} l.`
    }
    if (reading === '' || !Number.isInteger(Number(reading)) || Number(reading) < currentReading) {
      validation.mileageOrHours = `Podaj liczbę całkowitą, co najmniej ${currentReading}.`
    }
    setErrors(validation)
    setFormError(null)
    if (Object.keys(validation).length === 0) {
      save.mutate({
        type,
        fuelLevel: hasTank ? fuelValue : null,
        mileageOrHours: Number(reading),
        notes: notes.trim() || null,
      })
    }
  }

  return (
    <Dialog open={open} onClose={save.isPending ? undefined : onClose} fullWidth maxWidth="xs">
      <form onSubmit={handleSubmit} noValidate>
        <DialogTitle>{type === 'ISSUE' ? 'Protokół wydania' : 'Protokół zwrotu'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {formError && <Alert severity="error">{formError}</Alert>}
            {hasTank && (
              <TextField
                label={`Paliwo (l, zbiornik ${formatNumber(tankCapacity)} l)`}
                type="number"
                value={fuel}
                onChange={(event) => setFuel(event.target.value)}
                error={Boolean(errors.fuelLevel)}
                helperText={errors.fuelLevel}
                slotProps={{ htmlInput: { min: 0, max: tankCapacity, step: 0.01 } }}
              />
            )}
            <TextField
              label={counterLabel(categoryType)}
              type="number"
              value={reading}
              onChange={(event) => setReading(event.target.value)}
              error={Boolean(errors.mileageOrHours)}
              helperText={errors.mileageOrHours ?? `Obecny stan: ${formatNumber(currentReading)}`}
              slotProps={{ htmlInput: { min: currentReading, step: 1 } }}
            />
            <TextField
              label="Uwagi"
              multiline
              minRows={2}
              value={notes}
              onChange={(event) => setNotes(event.target.value)}
              error={Boolean(errors.notes)}
              helperText={errors.notes}
              slotProps={{ htmlInput: { maxLength: 2000 } }}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={save.isPending}>
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
