import { api } from './client'

export type CategoryType = 'CAR' | 'EXCAVATOR' | 'MOWER' | 'TRACTOR' | 'CONSTRUCTION'
export type UnitStatus = 'AVAILABLE' | 'IN_SERVICE'

export interface Category {
  id: number
  name: string
  type: CategoryType
  requiredParams: string[]
}

export interface Equipment {
  id: number
  name: string
  categoryId: number
  categoryName: string
  categoryType: CategoryType
  dailyPrice: number
  deposit: number
  tankCapacity: number | null
  params: Record<string, string | number | boolean>
}

export interface Unit {
  id: number
  equipmentId: number
  equipmentName: string
  serialOrPlate: string
  status: UnitStatus
  mileageOrHours: number
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

// Wartości jak w URL; puste pola nie trafiają do zapytania
export interface EquipmentQuery {
  categoryId?: string
  q?: string
  minPrice?: string
  maxPrice?: string
  from?: string
  to?: string
  sort?: string
  page: number
  size: number
}

export async function getCategories(): Promise<Category[]> {
  const { data } = await api.get<Category[]>('/categories')
  return data
}

export async function getEquipmentPage(query: EquipmentQuery): Promise<PageResponse<Equipment>> {
  const params = Object.fromEntries(Object.entries(query).filter(([, value]) => value !== undefined && value !== ''))
  const { data } = await api.get<PageResponse<Equipment>>('/equipment', { params })
  return data
}

export async function getEquipment(id: number): Promise<Equipment> {
  const { data } = await api.get<Equipment>(`/equipment/${id}`)
  return data
}

export async function getUnits(equipmentId: number): Promise<Unit[]> {
  const { data } = await api.get<Unit[]>('/units', { params: { equipmentId } })
  return data
}
