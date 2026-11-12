import { api } from './client'

export type CategoryType = 'CAR' | 'EXCAVATOR' | 'MOWER' | 'TRACTOR' | 'CONSTRUCTION'

export interface Category {
  id: number
  name: string
  type: CategoryType
  requiredParams: string[]
}

export async function getCategories(): Promise<Category[]> {
  const { data } = await api.get<Category[]>('/categories')
  return data
}
