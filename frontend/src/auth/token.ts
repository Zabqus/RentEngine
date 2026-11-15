import type { Role } from '../api/auth'

interface TokenPayload {
  sub: string
  email: string
  role: Role
  exp: number
}

export function decodeToken(token: string): TokenPayload | null {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(atob(payload)) as TokenPayload
  } catch {
    return null
  }
}

export function isExpired(payload: TokenPayload): boolean {
  return payload.exp * 1000 <= Date.now()
}
