import { createContext } from 'react'

export interface Notify {
  success: (message: string) => void
  error: (message: string) => void
  // Komunikat z błędu API (ProblemDetail)
  apiError: (error: unknown) => void
}

export const NotificationContext = createContext<Notify | null>(null)
