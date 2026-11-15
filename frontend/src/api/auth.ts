import { api } from './client'

export type Role = 'CLIENT' | 'EMPLOYEE' | 'ADMIN'

export interface User {
  id: number
  email: string
  firstName: string
  lastName: string
  role: Role
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest extends LoginRequest {
  firstName: string
  lastName: string
}

export async function login(request: LoginRequest): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/auth/login', request)
  return data
}

export async function register(request: RegisterRequest): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/auth/register', request)
  return data
}
