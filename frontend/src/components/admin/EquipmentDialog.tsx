import { useState } from 'react'
import { MenuItem, Stack, TextField, Typography } from '@mui/material'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createEquipment, updateEquipment, type Equipment, type EquipmentRequest } from '../../api/catalog'
import { getApiError } from '../../api/errors'
import { useCategories } from '../../api/useCategories'
import { useNotify } from '../../notifications/useNotify'
import { paramLabel } from '../../utils/format'
import { FormDialog } from './FormDialog'

interface Props {
  // null = nowy sprzęt
  equipment: Equipment | null
  onClose: () => void
}

type Errors = Record<string, string | undefined>

// Liczby wysyłamy jako liczby, resztę jako tekst (np. gearbox: MANUAL)
function parseParam(value: string): string | number {
  const trimmed = value.trim().replace(',', '.')
  return /^-?\d+(\.\d+)?$/.test(trimmed) ? Number(trimmed) : value.trim()
}

export function EquipmentDialog({ equipment, onClose }: Props) {
  const notify = useNotify()
  const queryClient = useQueryClient()
  const categories = useCategories()

  const [name, setName] = useState(equipment?.name ?? '')
  const [categoryId, setCategoryId] = useState(equipment ? String(equipment.categoryId) : '')
  const [dailyPrice, setDailyPrice] = useState(equipment ? String(equipment.dailyPrice) : '')
  const [deposit, setDeposit] = useState(equipment ? String(equipment.deposit) : '')
  const [tankCapacity, setTankCapacity] = useState(equipment?.tankCapacity != null ? String(equipment.tankCapacity) : '')
  const [params, setParams] = useState<Record<string, string>>(() =>
    Object.fromEntries(Object.entries(equipment?.params ?? {}).map(([key, value]) => [key, String(value)])),
  )
  const [errors, setErrors] = useState<Errors>({})
  const [formError, setFormError] = useState<string | null>(null)

  const category = categories.data?.find((item) => String(item.id) === categoryId)
  // Wymagane parametry kategorii + dodatkowe, które sprzęt już ma
  const paramKeys = [...new Set([...(category?.requiredParams ?? []), ...Object.keys(params)])]

  const save = useMutation({
    mutationFn: (request: EquipmentRequest) =>
      equipment ? updateEquipment(equipment.id, request) : createEquipment(request),
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: ['equipment'] })
      notify.success(equipment ? `Zapisano „${saved.name}”.` : `Dodano „${saved.name}”.`)
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
    if (!name.trim()) validation.name = 'Podaj nazwę.'
    if (!categoryId) validation.categoryId = 'Wybierz kategorię.'
    if (!(Number(dailyPrice) > 0)) validation.dailyPrice = 'Cena musi być większa od 0.'
    if (deposit === '' || Number(deposit) < 0) validation.deposit = 'Podaj kaucję (0 lub więcej).'
    if (tankCapacity !== '' && !(Number(tankCapacity) > 0)) validation.tankCapacity = 'Pojemność musi być większa od 0.'
    category?.requiredParams.forEach((key) => {
      if (!params[key]?.trim()) validation[`params.${key}`] = 'Parametr wymagany dla tej kategorii.'
    })
    setErrors(validation)
    setFormError(null)
    if (Object.keys(validation).length > 0) return

    const cleanParams = Object.fromEntries(
      Object.entries(params)
        .filter(([, value]) => value.trim() !== '')
        .map(([key, value]) => [key, parseParam(value)]),
    )
    save.mutate({
      name: name.trim(),
      categoryId: Number(categoryId),
      dailyPrice: Number(dailyPrice),
      deposit: Number(deposit),
      tankCapacity: tankCapacity === '' ? null : Number(tankCapacity),
      params: cleanParams,
    })
  }

  const errorProps = (key: string) => ({ error: Boolean(errors[key]), helperText: errors[key] })
  const clear = (key: string) => setErrors((current) => ({ ...current, [key]: undefined }))

  return (
    <FormDialog
      title={equipment ? `Edycja: ${equipment.name}` : 'Nowy sprzęt'}
      pending={save.isPending}
      error={formError}
      onSubmit={handleSubmit}
      onClose={onClose}
    >
      <TextField label="Nazwa" value={name} onChange={(e) => { setName(e.target.value); clear('name') }} {...errorProps('name')} />
      <TextField
        select
        label="Kategoria"
        value={categoryId}
        onChange={(e) => { setCategoryId(e.target.value); clear('categoryId') }}
        {...errorProps('categoryId')}
      >
        {categories.data?.map((item) => (
          <MenuItem key={item.id} value={String(item.id)}>
            {item.name}
          </MenuItem>
        ))}
      </TextField>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
        <TextField
          label="Cena za dobę (zł)"
          type="number"
          fullWidth
          value={dailyPrice}
          onChange={(e) => { setDailyPrice(e.target.value); clear('dailyPrice') }}
          {...errorProps('dailyPrice')}
        />
        <TextField
          label="Kaucja (zł)"
          type="number"
          fullWidth
          value={deposit}
          onChange={(e) => { setDeposit(e.target.value); clear('deposit') }}
          {...errorProps('deposit')}
        />
        <TextField
          label="Zbiornik (l)"
          type="number"
          fullWidth
          value={tankCapacity}
          onChange={(e) => { setTankCapacity(e.target.value); clear('tankCapacity') }}
          {...errorProps('tankCapacity')}
          helperText={errors.tankCapacity ?? 'Puste = bez zbiornika'}
        />
      </Stack>
      {paramKeys.length > 0 && (
        <>
          <Typography variant="subtitle2" color="text.secondary">
            Parametry
          </Typography>
          <Stack direction="row" useFlexGap spacing={2} sx={{ flexWrap: 'wrap' }}>
            {paramKeys.map((key) => (
              <TextField
                key={key}
                label={paramLabel(key)}
                value={params[key] ?? ''}
                onChange={(e) => {
                  setParams({ ...params, [key]: e.target.value })
                  clear(`params.${key}`)
                }}
                required={category?.requiredParams.includes(key)}
                sx={{ flex: '1 1 180px' }}
                {...errorProps(`params.${key}`)}
              />
            ))}
          </Stack>
        </>
      )}
    </FormDialog>
  )
}
