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

export async function getUnit(id: number): Promise<Unit> {
  const { data } = await api.get<Unit>(`/units/${id}`)
  return data
}

export async function getUnits(equipmentId: number): Promise<Unit[]> {
  const { data } = await api.get<Unit[]>('/units', { params: { equipmentId } })
  return data
}

// --- Zarządzanie katalogiem (ADMIN; zmiana statusu egzemplarza także EMPLOYEE) ---

export interface EquipmentRequest {
  name: string
  categoryId: number
  dailyPrice: number
  deposit: number
  tankCapacity: number | null
  params: Record<string, string | number | boolean>
}

export interface UnitRequest {
  equipmentId: number
  serialOrPlate: string
  mileageOrHours: number
}

export async function createEquipment(request: EquipmentRequest): Promise<Equipment> {
  const { data } = await api.post<Equipment>('/equipment', request)
  return data
}

export async function updateEquipment(id: number, request: EquipmentRequest): Promise<Equipment> {
  const { data } = await api.put<Equipment>(`/equipment/${id}`, request)
  return data
}

export async function deleteEquipment(id: number): Promise<void> {
  await api.delete(`/equipment/${id}`)
}

export async function createUnit(request: UnitRequest): Promise<Unit> {
  const { data } = await api.post<Unit>('/units', request)
  return data
}

export async function updateUnit(id: number, request: UnitRequest): Promise<Unit> {
  const { data } = await api.put<Unit>(`/units/${id}`, request)
  return data
}

export async function changeUnitStatus(id: number, status: UnitStatus): Promise<Unit> {
  const { data } = await api.patch<Unit>(`/units/${id}/status`, { status })
  return data
}

export async function deleteUnit(id: number): Promise<void> {
  await api.delete(`/units/${id}`)
}
