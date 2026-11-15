import type { ChipProps } from '@mui/material'
import type { AgreementStatus } from '../api/rentals'

export const STATUS_LABELS: Record<AgreementStatus, string> = {
  RESERVED: 'Zarezerwowana',
  ACTIVE: 'Wydana',
  OVERDUE: 'Przeterminowana',
  RETURNED: 'Zwrócona',
  SETTLED: 'Rozliczona',
  CANCELLED: 'Anulowana',
}

export const STATUS_COLORS: Record<AgreementStatus, ChipProps['color']> = {
  RESERVED: 'info',
  ACTIVE: 'primary',
  OVERDUE: 'error',
  RETURNED: 'warning',
  SETTLED: 'success',
  CANCELLED: 'default',
}

export const ALL_STATUSES = Object.keys(STATUS_LABELS) as AgreementStatus[]
