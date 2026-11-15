import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import * as authApi from '../api/auth'
import type { User } from '../api/auth'
import { setAuthToken, setUnauthorizedHandler } from '../api/client'
import { AuthContext, type AuthContextValue } from './context'
import { decodeToken, isExpired } from './token'

const STORAGE_KEY = 'rentengine.auth'

interface Session {
  token: string
  user: User
  expiresAt: number
}

function toSession(token: string, user: User): Session | null {
  const payload = decodeToken(token)
  if (!payload || isExpired(payload)) {
    return null
  }
  // Rola z tokena, bo to ją sprawdza backend
  return { token, user: { ...user, role: payload.role }, expiresAt: payload.exp * 1000 }
}

function loadSession(): Session | null {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (!stored) {
      return null
    }
    const { token, user } = JSON.parse(stored) as Session
    return toSession(token, user)
  } catch {
    return null
  }
}

function initialSession(): Session | null {
  const session = loadSession()
  // Token musi być w kliencie przed pierwszymi zapytaniami dzieci
  setAuthToken(session?.token ?? null)
  return session
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(initialSession)
  const queryClient = useQueryClient()

  const startSession = useCallback((token: string, user: User) => {
    const next = toSession(token, user)
    if (!next) {
      throw new Error('Invalid token')
    }
    setAuthToken(next.token)
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ token: next.token, user: next.user }))
    } catch {
      // Bez localStorage sesja działa do odświeżenia strony
    }
    setSession(next)
    return next.user
  }, [])

  const logout = useCallback(() => {
    setAuthToken(null)
    try {
      localStorage.removeItem(STORAGE_KEY)
    } catch {
      // ignorujemy
    }
    setSession(null)
    // Dane poprzedniego użytkownika nie mogą zostać w cache
    queryClient.clear()
  }, [queryClient])

  useEffect(() => {
    setUnauthorizedHandler(logout)
    return () => setUnauthorizedHandler(null)
  }, [logout])

  // Wylogowanie w chwili wygaśnięcia tokena
  useEffect(() => {
    if (!session) {
      return
    }
    const timeout = setTimeout(logout, Math.max(session.expiresAt - Date.now(), 0))
    return () => clearTimeout(timeout)
  }, [session, logout])

  const value = useMemo<AuthContextValue>(
    () => ({
      user: session?.user ?? null,
      role: session?.user.role ?? null,
      login: async (request) => {
        const response = await authApi.login(request)
        return startSession(response.accessToken, response.user)
      },
      register: async (request) => {
        const response = await authApi.register(request)
        return startSession(response.accessToken, response.user)
      },
      logout,
    }),
    [session, startSession, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
