import { useState, type ChangeEvent } from 'react'
import { Link, Stack, TextField } from '@mui/material'
import { Link as RouterLink, Navigate, useNavigate } from 'react-router'
import type { RegisterRequest } from '../api/auth'
import { getApiError } from '../api/errors'
import { ROLE_HOME } from '../auth/roles'
import { useAuth } from '../auth/useAuth'
import { AuthCard } from '../components/AuthCard'

type Field = keyof RegisterRequest
type FieldErrors = Partial<Record<Field, string>>

const EMPTY: RegisterRequest = { firstName: '', lastName: '', email: '', password: '' }

// Te same limity co RegisterRequest w backendzie
function validate(form: RegisterRequest): FieldErrors {
  const errors: FieldErrors = {}
  if (!form.firstName.trim()) errors.firstName = 'Podaj imię.'
  if (!form.lastName.trim()) errors.lastName = 'Podaj nazwisko.'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) errors.email = 'Podaj poprawny adres e-mail.'
  if (form.password.length < 8 || form.password.length > 72) errors.password = 'Hasło musi mieć od 8 do 72 znaków.'
  return errors
}

export function RegisterPage() {
  const { role, register } = useAuth()
  const navigate = useNavigate()

  const [form, setForm] = useState<RegisterRequest>(EMPTY)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (role && !pending) {
    return <Navigate to={ROLE_HOME[role]} replace />
  }

  const update = (field: Field) => (event: ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [field]: event.target.value })
    setFieldErrors({ ...fieldErrors, [field]: undefined })
  }

  const handleSubmit = async () => {
    const errors = validate(form)
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) {
      setError(null)
      return
    }
    setPending(true)
    setError(null)
    try {
      const user = await register({ ...form, email: form.email.trim() })
      navigate(ROLE_HOME[user.role], { replace: true })
    } catch (e) {
      const apiError = getApiError(e)
      setError(apiError.message)
      // Błędy walidacji z backendu (400) przy polach, po angielsku jak w API
      setFieldErrors(apiError.fieldErrors as FieldErrors)
      setPending(false)
    }
  }

  const fieldProps = (field: Field) => ({
    value: form[field],
    onChange: update(field),
    error: Boolean(fieldErrors[field]),
    helperText: fieldErrors[field],
    required: true,
  })

  return (
    <AuthCard
      title="Załóż konto"
      submitLabel="Zarejestruj"
      pending={pending}
      error={error}
      onSubmit={handleSubmit}
      footer={
        <>
          Masz już konto?{' '}
          <Link component={RouterLink} to="/login">
            Zaloguj się
          </Link>
        </>
      }
    >
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
        <TextField label="Imię" autoComplete="given-name" autoFocus fullWidth {...fieldProps('firstName')} />
        <TextField label="Nazwisko" autoComplete="family-name" fullWidth {...fieldProps('lastName')} />
      </Stack>
      <TextField label="E-mail" type="email" autoComplete="email" {...fieldProps('email')} />
      <TextField label="Hasło" type="password" autoComplete="new-password" {...fieldProps('password')} />
    </AuthCard>
  )
}
