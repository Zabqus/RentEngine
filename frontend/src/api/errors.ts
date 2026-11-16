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
  status: number | null
  code: string | null
  message: string
  fieldErrors: Record<string, string>
}

// Kody reguł biznesowych (409) z backendu
const CODE_MESSAGES: Record<string, string> = {
  EMAIL_TAKEN: 'Ten adres e-mail jest już zarejestrowany.',
  UNIT_NOT_AVAILABLE: 'Ten egzemplarz jest już zarezerwowany w wybranym terminie. Wybierz inny termin lub egzemplarz.',
  UNIT_IN_SERVICE: 'Egzemplarz jest w serwisie i nie można go teraz wypożyczyć.',
  UNIT_RENTED_OUT: 'Egzemplarz jest obecnie wypożyczony.',
  UNPAID_SETTLEMENT: 'Masz nieopłacone rozliczenie. Ureguluj je, zanim zarezerwujesz kolejny sprzęt.',
  INVALID_STATUS_TRANSITION: 'Tej operacji nie można wykonać przy obecnym statusie umowy.',
  TOO_EARLY_TO_ISSUE: 'Sprzęt można wydać najwcześniej w dniu rozpoczęcia umowy.',
  RENTAL_PERIOD_ENDED: 'Okres umowy już minął.',
  MAJOR_DAMAGE_ON_ISSUE: 'Przy wydaniu nie można zgłosić poważnego uszkodzenia.',
  AGREEMENT_SETTLED: 'Umowa jest już rozliczona.',
  AGREEMENT_NOT_RETURNED: 'Rozliczenie można wygenerować dopiero po zwrocie sprzętu.',
  SETTLEMENT_EXISTS: 'Rozliczenie tej umowy już istnieje.',
  SETTLEMENT_ALREADY_PAID: 'Rozliczenie jest już opłacone.',
  CATEGORY_IN_USE: 'Kategoria ma przypisany sprzęt i nie można jej usunąć.',
  CATEGORY_NAME_TAKEN: 'Kategoria o tej nazwie już istnieje.',
  CATEGORY_PARAMS_CONFLICT: 'Istniejący sprzęt nie ma wymaganego parametru.',
  EQUIPMENT_HAS_UNITS: 'Sprzęt ma egzemplarze i nie można go usunąć.',
  UNIT_HAS_AGREEMENTS: 'Egzemplarz ma umowy i nie można go usunąć.',
  UNIT_SERIAL_TAKEN: 'Egzemplarz o tym numerze już istnieje.',
  CANNOT_CHANGE_OWN_ROLE: 'Nie możesz zmienić własnej roli.',
  CANNOT_DELETE_SELF: 'Nie możesz usunąć własnego konta.',
  USER_HAS_AGREEMENTS: 'Użytkownik ma umowy i nie można go usunąć.',
}

const STATUS_MESSAGES: Record<number, string> = {
  400: 'Popraw zaznaczone pola.',
  401: 'Nieprawidłowy e-mail lub hasło.',
  403: 'Nie masz uprawnień do tej operacji.',
  404: 'Nie znaleziono zasobu.',
  413: 'Plik jest za duży (maks. 5 MB).',
  409: 'Ktoś właśnie zmienił te dane. Odśwież i spróbuj ponownie.',
}

// Komunikaty walidacji z backendu (po angielsku) → polski; nieznane zostają bez zmian
const FIELD_MESSAGES: [RegExp, string][] = [
  [/^must not be (blank|null|empty)$/, 'Pole jest wymagane.'],
  [/^must be a future date$/, 'Data musi być w przyszłości.'],
  [/^must be after (startDate|from)$/, 'Koniec musi być po początku.'],
  [/^must be a well-formed email address$/, 'Podaj poprawny adres e-mail.'],
  [/^must be greater than 0$/, 'Wartość musi być większa od 0.'],
  [/^must be greater than or equal to 0$/, 'Wartość nie może być ujemna.'],
  [/^size must be between (\d+) and (\d+)$/, 'Długość musi mieć od $1 do $2 znaków.'],
  [/^must be a number$/, 'Podaj liczbę.'],
  [/^must not exceed tank capacity \((.+) l\)$/, 'Nie może przekraczać pojemności zbiornika ($1 l).'],
  [/^must not be less than current unit reading \((\d+)\)$/, 'Nie może być mniejszy niż obecny stan licznika ($1).'],
  [/^must be empty, equipment has no fuel tank$/, 'Ten sprzęt nie ma zbiornika paliwa.'],
  [/^must be a JPEG or PNG image$/, 'Plik musi być obrazem JPEG lub PNG.'],
  [/^is required for category '(.+)'$/, 'Wymagany dla kategorii „$1”.'],
  [/^numeric value out of bounds.*$/, 'Za duża liczba lub za dużo miejsc po przecinku.'],
]

export function translateFieldMessage(message: string): string {
  for (const [pattern, translation] of FIELD_MESSAGES) {
    if (pattern.test(message)) {
      return message.replace(pattern, translation)
    }
  }
  return message
}

export function getApiError(error: unknown): ApiError {
  if (!isAxiosError<ApiProblem>(error) || !error.response) {
    return { status: null, code: null, message: 'Brak połączenia z serwerem.', fieldErrors: {} }
  }
  const status = error.response.status
  const problem = error.response.data ?? {}
  const message =
    (problem.code && CODE_MESSAGES[problem.code]) ||
    STATUS_MESSAGES[status] ||
    problem.detail ||
    'Wystąpił nieoczekiwany błąd.'
  const fieldErrors = Object.fromEntries(
    Object.entries(problem.errors ?? {}).map(([field, text]) => [field, translateFieldMessage(text)]),
  )
  return { status, code: problem.code ?? null, message, fieldErrors }
}
