import { useState, type ChangeEvent } from 'react'
import { MenuItem, Stack, TextField } from '@mui/material'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { Role, User } from '../../api/auth'
import { getApiError } from '../../api/errors'
import { createUser, updateUser, type UserRequest } from '../../api/users'
import { ROLE_LABELS } from '../../auth/roles'
import { useNotify } from '../../notifications/useNotify'
import { FormDialog } from './FormDialog'

interface Props {
  // null = nowy użytkownik
  user: User | null
  isSelf: boolean
  onClose: () => void
}

type Field = 'email' | 'password' | 'firstName' | 'lastName' | 'role'
type Errors = Partial<Record<Field, string>>

export function UserDialog({ user, isSelf, onClose }: Props) {
  const notify = useNotify()
  const queryClient = useQueryClient()
  const [form, setForm] = useState({
    email: user?.email ?? '',
    password: '',
    firstName: user?.firstName ?? '',
    lastName: user?.lastName ?? '',
    role: user?.role ?? ('CLIENT' as Role),
  })
  const [errors, setErrors] = useState<Errors>({})
  const [formError, setFormError] = useState<string | null>(null)

  const save = useMutation({
    mutationFn: (request: UserRequest) => (user ? updateUser(user.id, request) : createUser(request)),
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      notify.success(user ? `Zapisano ${saved.email}.` : `Dodano ${saved.email}.`)
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
    if (!form.firstName.trim()) validation.firstName = 'Podaj imię.'
    if (!form.lastName.trim()) validation.lastName = 'Podaj nazwisko.'
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) validation.email = 'Podaj poprawny adres e-mail.'
    // Przy edycji puste hasło = bez zmian
    if ((!user || form.password) && (form.password.length < 8 || form.password.length > 72)) {
      validation.password = 'Hasło musi mieć od 8 do 72 znaków.'
    }
    setErrors(validation)
    setFormError(null)
    if (Object.keys(validation).length === 0) {
      save.mutate({
        email: form.email.trim(),
        password: form.password || null,
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        role: form.role,
      })
    }
  }

  const field = (key: Field) => ({
    value: form[key],
    onChange: (event: ChangeEvent<HTMLInputElement>) => {
      setForm({ ...form, [key]: event.target.value })
      setErrors({ ...errors, [key]: undefined })
    },
    error: Boolean(errors[key]),
    helperText: errors[key],
    fullWidth: true,
  })

  return (
    <FormDialog
      title={user ? `Edycja: ${user.email}` : 'Nowy użytkownik'}
      pending={save.isPending}
      error={formError}
      onSubmit={handleSubmit}
      onClose={onClose}
    >
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
        <TextField label="Imię" {...field('firstName')} />
        <TextField label="Nazwisko" {...field('lastName')} />
      </Stack>
      <TextField label="E-mail" type="email" {...field('email')} />
      <TextField
        label={user ? 'Nowe hasło (opcjonalnie)' : 'Hasło'}
        type="password"
        autoComplete="new-password"
        {...field('password')}
        helperText={errors.password ?? (user ? 'Zostaw puste, żeby nie zmieniać' : undefined)}
      />
      <TextField
        select
        label="Rola"
        {...field('role')}
        disabled={isSelf}
        helperText={errors.role ?? (isSelf ? 'Nie możesz zmienić własnej roli' : 'Zmiana roli działa po ponownym zalogowaniu')}
      >
        {(Object.keys(ROLE_LABELS) as Role[]).map((role) => (
          <MenuItem key={role} value={role}>
            {ROLE_LABELS[role]}
          </MenuItem>
        ))}
      </TextField>
    </FormDialog>
  )
}
