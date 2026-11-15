import { isAxiosError } from 'axios'
import { api } from './client'
import type { PageResponse } from './catalog'

export type AgreementStatus = 'RESERVED' | 'ACTIVE' | 'OVERDUE' | 'RETURNED' | 'SETTLED' | 'CANCELLED'

export interface Rental {
  id: number
  clientId: number
  clientName: string
  unitId: number
  serialOrPlate: string
  equipmentId: number
  equipmentName: string
  startDate: string
  endDate: string
  status: AgreementStatus
  basePrice: number
  deposit: number
  createdAt: string
}

export interface RentalRequest {
  unitId: number
  startDate: string
  endDate: string
}

export interface Settlement {
  id: number
  agreementId: number
  baseAmount: number
  lateDays: number
  lateFee: number
  fuelFee: number
  damageCost: number
  depositDeduction: number
  damageFee: number
  deposit: number
  depositRefund: number
  total: number
  paid: boolean
  createdAt: string
  paidAt: string | null
}

export interface RentalQuery {
  status?: AgreementStatus | ''
  page: number
  size: number
  sort?: string
}

function cleanParams(query: object) {
  return Object.fromEntries(Object.entries(query).filter(([, value]) => value !== undefined && value !== ''))
}

export async function createRental(request: RentalRequest): Promise<Rental> {
  const { data } = await api.post<Rental>('/rentals', request)
  return data
}

export async function getMyRentals(query: RentalQuery): Promise<PageResponse<Rental>> {
  const { data } = await api.get<PageResponse<Rental>>('/rentals/my', { params: cleanParams(query) })
  return data
}

// Wszystkie umowy (EMPLOYEE/ADMIN)
export async function getRentals(query: RentalQuery): Promise<PageResponse<Rental>> {
  const { data } = await api.get<PageResponse<Rental>>('/rentals', { params: cleanParams(query) })
  return data
}

export async function getRental(id: number): Promise<Rental> {
  const { data } = await api.get<Rental>(`/rentals/${id}`)
  return data
}

export async function createSettlement(rentalId: number): Promise<Settlement> {
  const { data } = await api.post<Settlement>(`/rentals/${rentalId}/settlement`)
  return data
}

export async function paySettlement(settlementId: number): Promise<Settlement> {
  const { data } = await api.patch<Settlement>(`/settlements/${settlementId}/pay`)
  return data
}

export async function cancelRental(id: number): Promise<Rental> {
  const { data } = await api.patch<Rental>(`/rentals/${id}/cancel`)
  return data
}

// null = rozliczenie jeszcze nie powstało
export async function getSettlement(rentalId: number): Promise<Settlement | null> {
  try {
    const { data } = await api.get<Settlement>(`/rentals/${rentalId}/settlement`)
    return data
  } catch (error) {
    if (isAxiosError(error) && error.response?.status === 404) {
      return null
    }
    throw error
  }
}
