import { useState, type FormEvent } from 'react'
import { Alert, Box, Button, MenuItem, Stack, TextField, Typography } from '@mui/material'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import type { Equipment, Unit } from '../api/catalog'
import { getApiError } from '../api/errors'
import { createRental, type RentalRequest } from '../api/rentals'
import { useNotify } from '../notifications/useNotify'
import { formatMoney, rentalDays, toInputDateTime } from '../utils/format'

interface Props {
  equipment: Equipment
  units: Unit[]
}

type Field = 'unitId' | 'startDate' | 'endDate'

function defaultStart(): Date {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  date.setHours(10, 0, 0, 0)
  return date
}

export function ReservationForm({ equipment, units }: Props) {
  const notify = useNotify()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const available = units.filter((unit) => unit.status === 'AVAILABLE')

  const [unitId, setUnitId] = useState(available[0] ? String(available[0].id) : '')
  const [startDate, setStartDate] = useState(() => toInputDateTime(defaultStart()))
  const [endDate, setEndDate] = useState(() => {
    const end = defaultStart()
    end.setDate(end.getDate() + 1)
    return toInputDateTime(end)
  })
  const [errors, setErrors] = useState<Partial<Record<Field, string>>>({})
  const [formError, setFormError] = useState<string | null>(null)

  const days = rentalDays(startDate, endDate)

  const mutation = useMutation({
    mutationFn: (request: RentalRequest) => createRental(request),
    onSuccess: (rental) => {
      queryClient.invalidateQueries({ queryKey: ['rentals'] })
      queryClient.invalidateQueries({ queryKey: ['equipment'] })
      notify.success(`Rezerwacja nr ${rental.id} przyjęta. Potwierdzenie wyślemy e-mailem.`)
      navigate('/my-rentals')
    },
    onError: (error) => {
      const apiError = getApiError(error)
      setErrors(apiError.fieldErrors)
      setFormError(apiError.message)
      notify.error(apiError.message)
    },
  })

  if (available.length === 0) {
    return <Alert severity="warning">Wszystkie egzemplarze są w serwisie. Spróbuj później.</Alert>
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    const validation: Partial<Record<Field, string>> = {}
    if (!unitId) validation.unitId = 'Wybierz egzemplarz.'
    if (!startDate) validation.startDate = 'Podaj początek.'
    else if (new Date(startDate) <= new Date()) validation.startDate = 'Początek musi być w przyszłości.'
    if (!endDate) validation.endDate = 'Podaj koniec.'
    else if (days === 0) validation.endDate = 'Koniec musi być po początku.'
    setErrors(validation)
    setFormError(null)
    if (Object.keys(validation).length === 0) {
      mutation.mutate({ unitId: Number(unitId), startDate, endDate })
    }
  }

  const clearError = (field: Field) => setErrors((current) => ({ ...current, [field]: undefined }))

  return (
    <Stack component="form" spacing={2} noValidate onSubmit={handleSubmit}>
      <Typography variant="h6">Zarezerwuj</Typography>
      {formError && <Alert severity="error">{formError}</Alert>}
      <TextField
        select
        label="Egzemplarz"
        size="small"
        value={unitId}
        onChange={(event) => {
          setUnitId(event.target.value)
          clearError('unitId')
        }}
        error={Boolean(errors.unitId)}
        helperText={errors.unitId}
      >
        {available.map((unit) => (
          <MenuItem key={unit.id} value={String(unit.id)}>
            {unit.serialOrPlate}
          </MenuItem>
        ))}
      </TextField>
      <TextField
        label="Odbiór"
        type="datetime-local"
        size="small"
        value={startDate}
        onChange={(event) => {
          setStartDate(event.target.value)
          clearError('startDate')
        }}
        error={Boolean(errors.startDate)}
        helperText={errors.startDate}
        slotProps={{ inputLabel: { shrink: true } }}
      />
      <TextField
        label="Zwrot"
        type="datetime-local"
        size="small"
        value={endDate}
        onChange={(event) => {
          setEndDate(event.target.value)
          clearError('endDate')
        }}
        error={Boolean(errors.endDate)}
        helperText={errors.endDate}
        slotProps={{ inputLabel: { shrink: true } }}
      />
      {days > 0 && (
        <Box>
          <Typography>
            {days} {days === 1 ? 'doba' : 'doby'} × {formatMoney(equipment.dailyPrice)} ={' '}
            <strong>{formatMoney(days * equipment.dailyPrice)}</strong>
          </Typography>
          <Typography variant="body2" color="text.secondary">
            + zwrotna kaucja {formatMoney(equipment.deposit)}. Każda rozpoczęta doba liczy się w całości.
          </Typography>
        </Box>
      )}
      <Button type="submit" variant="contained" size="large" loading={mutation.isPending}>
        Rezerwuję
      </Button>
    </Stack>
  )
}
