import { createContext } from 'react'
import type { LoginRequest, RegisterRequest, Role, User } from '../api/auth'

export interface AuthContextValue {
  user: User | null
  role: Role | null
  login: (request: LoginRequest) => Promise<User>
  register: (request: RegisterRequest) => Promise<User>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
