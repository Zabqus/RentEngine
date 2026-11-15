import { useState } from 'react'
import { Link, TextField } from '@mui/material'
import { Link as RouterLink, Navigate, useLocation, useNavigate } from 'react-router'
import { getApiError } from '../api/errors'
import { ROLE_HOME } from '../auth/roles'
import { useAuth } from '../auth/useAuth'
import { AuthCard } from '../components/AuthCard'

export function LoginPage() {
  const { role, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (role && !pending) {
    return <Navigate to={ROLE_HOME[role]} replace />
  }

  const handleSubmit = async () => {
    if (!email.trim() || !password) {
      setError('Podaj e-mail i hasło.')
      return
    }
    setPending(true)
    setError(null)
    try {
      const user = await login({ email: email.trim(), password })
      navigate(from ?? ROLE_HOME[user.role], { replace: true })
    } catch (e) {
      setError(getApiError(e).message)
      setPending(false)
    }
  }

  return (
    <AuthCard
      title="Logowanie"
      submitLabel="Zaloguj"
      pending={pending}
      error={error}
      onSubmit={handleSubmit}
      footer={
        <>
          Nie masz konta?{' '}
          <Link component={RouterLink} to="/register">
            Zarejestruj się
          </Link>
        </>
      }
    >
      <TextField
        label="E-mail"
        type="email"
        autoComplete="email"
        autoFocus
        required
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />
      <TextField
        label="Hasło"
        type="password"
        autoComplete="current-password"
        required
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />
    </AuthCard>
  )
}
