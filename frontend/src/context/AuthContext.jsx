import { createContext, useContext, useState, useCallback } from 'react'
import { api } from '../api/client'
import { getStoredUser, storeSession, clearSession, getAccessToken } from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(getStoredUser())

  const login = useCallback(async (email, password) => {
    const data = await api.login({ email, password })
    storeSession(data)
    setUser(data.user)
    return data.user
  }, [])

  const register = useCallback(async (payload) => {
    const data = await api.register(payload)
    storeSession(data)
    setUser(data.user)
    return data.user
  }, [])

  const logout = useCallback(() => {
    clearSession()
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!getAccessToken(), login, register, logout, isAdmin: user?.role === 'ADMIN' || user?.role === 'SUPER_ADMIN' }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
