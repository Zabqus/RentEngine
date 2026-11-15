import type { Role } from '../api/auth'

export const ROLE_LABELS: Record<Role, string> = {
  CLIENT: 'Klient',
  EMPLOYEE: 'Pracownik',
  ADMIN: 'Administrator',
}

// Strona startowa po zalogowaniu
export const ROLE_HOME: Record<Role, string> = {
  CLIENT: '/my-rentals',
  EMPLOYEE: '/rentals',
  ADMIN: '/admin',
}
