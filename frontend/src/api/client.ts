import axios from 'axios'

export const api = axios.create({
  baseURL: '/api',
})

// AuthContext ustawia token i reakcję na 401; klient nie zna Reacta
let authToken: string | null = null
let onUnauthorized: (() => void) | null = null

export function setAuthToken(token: string | null) {
  authToken = token
}

export function setUnauthorizedHandler(handler: (() => void) | null) {
  onUnauthorized = handler
}

api.interceptors.request.use((config) => {
  if (authToken) {
    config.headers.Authorization = `Bearer ${authToken}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    // 401 przy logowaniu to złe hasło, a nie wygasła sesja
    const isAuthCall = error.config?.url?.startsWith('/auth/')
    if (error.response?.status === 401 && authToken && !isAuthCall) {
      onUnauthorized?.()
    }
    return Promise.reject(error)
  },
)
