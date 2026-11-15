import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import type { Role } from '../api/auth'
import { ForbiddenPage } from '../pages/ForbiddenPage'
import { useAuth } from './useAuth'

interface Props {
  roles?: Role[]
  children: ReactNode
}

export function RequireAuth({ roles, children }: Props) {
  const { role } = useAuth()
  const location = useLocation()

  if (!role) {
    // Po zalogowaniu wracamy na stronę, którą użytkownik chciał otworzyć
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  }
  if (roles && !roles.includes(role)) {
    return <ForbiddenPage />
  }
  return children
}
