import type { Role, User } from './auth'
import { api } from './client'

export interface UserRequest {
  email: string
  // Przy edycji null = hasło bez zmian
  password: string | null
  firstName: string
  lastName: string
  role: Role
}

export async function getUsers(): Promise<User[]> {
  const { data } = await api.get<User[]>('/users')
  return data
}

export async function createUser(request: UserRequest): Promise<User> {
  const { data } = await api.post<User>('/users', request)
  return data
}

export async function updateUser(id: number, request: UserRequest): Promise<User> {
  const { data } = await api.put<User>(`/users/${id}`, request)
  return data
}

export async function deleteUser(id: number): Promise<void> {
  await api.delete(`/users/${id}`)
}
