import { api } from './client'

export type ProtocolType = 'ISSUE' | 'RETURN'
export type DamageSeverity = 'MINOR' | 'MAJOR'

export interface Damage {
  id: number
  protocolId: number
  description: string
  severity: DamageSeverity
  cost: number
  photoUrl: string | null
}

export interface Protocol {
  id: number
  agreementId: number
  type: ProtocolType
  fuelLevel: number | null
  mileageOrHours: number
  notes: string | null
  employeeId: number
  employeeName: string
  createdAt: string
  damages: Damage[]
}

export interface ProtocolRequest {
  type: ProtocolType
  fuelLevel: number | null
  mileageOrHours: number
  notes: string | null
}

export interface DamageRequest {
  description: string
  severity: DamageSeverity
  cost: number
}

export async function getProtocols(rentalId: number): Promise<Protocol[]> {
  const { data } = await api.get<Protocol[]>(`/rentals/${rentalId}/protocols`)
  return data
}

export async function createProtocol(rentalId: number, request: ProtocolRequest): Promise<Protocol> {
  const { data } = await api.post<Protocol>(`/rentals/${rentalId}/protocols`, request)
  return data
}

export async function addDamage(protocolId: number, request: DamageRequest): Promise<Damage> {
  const { data } = await api.post<Damage>(`/protocols/${protocolId}/damages`, request)
  return data
}

export async function uploadDamagePhoto(damageId: number, file: File): Promise<Damage> {
  const form = new FormData()
  form.append('photo', file)
  const { data } = await api.put<Damage>(`/damages/${damageId}/photo`, form)
  return data
}

// Zdjęcie wymaga tokena, więc <img src> nie wystarczy: pobieramy blob przez axios
export async function getDamagePhoto(damageId: number): Promise<Blob> {
  const { data } = await api.get<Blob>(`/damages/${damageId}/photo`, { responseType: 'blob' })
  return data
}
