import { useState } from 'react'
import { TextField } from '@mui/material'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createUnit, updateUnit, type CategoryType, type Unit, type UnitRequest } from '../../api/catalog'
import { getApiError } from '../../api/errors'
import { useNotify } from '../../notifications/useNotify'
import { counterLabel } from '../../utils/format'
import { FormDialog } from './FormDialog'

interface Props {
  equipmentId: number
  categoryType: CategoryType
  // null = nowy egzemplarz
  unit: Unit | null
  onClose: () => void
}

type Errors = Partial<Record<'serialOrPlate' | 'mileageOrHours', string>>

export function UnitDialog({ equipmentId, categoryType, unit, onClose }: Props) {
  const notify = useNotify()
  const queryClient = useQueryClient()
  const [serial, setSerial] = useState(unit?.serialOrPlate ?? '')
  const [reading, setReading] = useState(unit ? String(unit.mileageOrHours) : '0')
  const [errors, setErrors] = useState<Errors>({})
  const [formError, setFormError] = useState<string | null>(null)

  const save = useMutation({
    mutationFn: (request: UnitRequest) => (unit ? updateUnit(unit.id, request) : createUnit(request)),
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: ['units'] })
      notify.success(unit ? `Zapisano egzemplarz ${saved.serialOrPlate}.` : `Dodano egzemplarz ${saved.serialOrPlate}.`)
      onClose()
    },
    onError: (error) => {
      const apiError = getApiError(error)
      setErrors(apiError.fieldErrors)
      setFormError(apiError.message)
    },
  })

  const handleSubmit = () => {
    const validation: Errors = {}
    if (!serial.trim()) validation.serialOrPlate = 'Podaj numer seryjny lub rejestrację.'
    if (reading === '' || !Number.isInteger(Number(reading)) || Number(reading) < 0) {
      validation.mileageOrHours = 'Podaj liczbę całkowitą, 0 lub więcej.'
    }
    setErrors(validation)
    setFormError(null)
    if (Object.keys(validation).length === 0) {
      save.mutate({ equipmentId, serialOrPlate: serial.trim(), mileageOrHours: Number(reading) })
    }
  }

  return (
    <FormDialog
      title={unit ? `Egzemplarz ${unit.serialOrPlate}` : 'Nowy egzemplarz'}
      pending={save.isPending}
      error={formError}
      onSubmit={handleSubmit}
      onClose={onClose}
    >
      <TextField
        label={categoryType === 'CAR' ? 'Numer rejestracyjny' : 'Numer seryjny'}
        value={serial}
        onChange={(e) => setSerial(e.target.value)}
        error={Boolean(errors.serialOrPlate)}
        helperText={errors.serialOrPlate ?? 'Zapisywany wielkimi literami'}
        slotProps={{ htmlInput: { maxLength: 50 } }}
      />
      <TextField
        label={counterLabel(categoryType)}
        type="number"
        value={reading}
        onChange={(e) => setReading(e.target.value)}
        error={Boolean(errors.mileageOrHours)}
        helperText={errors.mileageOrHours}
        slotProps={{ htmlInput: { min: 0, step: 1 } }}
      />
    </FormDialog>
  )
}
