import { isAxiosError } from 'axios'

// Format błędu z backendu (ProblemDetail)
export interface ApiProblem {
  title?: string
  status?: number
  detail?: string
  code?: string
  errors?: Record<string, string>
}

export interface ApiError {
  message: string
  fieldErrors: Record<string, string>
}

const CODE_MESSAGES: Record<string, string> = {
  EMAIL_TAKEN: 'Ten adres e-mail jest już zarejestrowany.',
}

const STATUS_MESSAGES: Record<number, string> = {
  400: 'Popraw zaznaczone pola.',
  401: 'Nieprawidłowy e-mail lub hasło.',
  403: 'Nie masz uprawnień do tej operacji.',
  404: 'Nie znaleziono zasobu.',
  409: 'Operacja jest niezgodna z regułami wypożyczalni.',
}

export function getApiError(error: unknown): ApiError {
  if (!isAxiosError<ApiProblem>(error) || !error.response) {
    return { message: 'Brak połączenia z serwerem.', fieldErrors: {} }
  }
  const problem = error.response.data ?? {}
  const message =
    (problem.code && CODE_MESSAGES[problem.code]) ||
    STATUS_MESSAGES[error.response.status] ||
    problem.detail ||
    'Wystąpił nieoczekiwany błąd.'
  return { message, fieldErrors: problem.errors ?? {} }
}
