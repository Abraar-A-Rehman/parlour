import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api } from '../api/api'
import { applySession, getStoredRefreshToken, refreshSession, subscribeSession } from '../api/client'
import type { SessionUser, TokenResponse } from '../types'
import { AuthContext, type AuthContextValue } from './context'

function toUser(tokens: TokenResponse | null): SessionUser | null {
  return tokens ? { email: tokens.email, fullName: tokens.fullName, role: tokens.role } : null
}

export default function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<SessionUser | null>(null)
  const [loading, setLoading] = useState<boolean>(() => getStoredRefreshToken() !== null)

  // Keep React state in sync with every token change (login, silent refresh, logout, expiry).
  useEffect(() => subscribeSession((tokens) => setUser(toUser(tokens))), [])

  // After a page reload: use the stored refresh token to get a fresh access token.
  useEffect(() => {
    if (getStoredRefreshToken() === null) {
      return
    }
    void refreshSession().finally(() => setLoading(false))
  }, [])

  const login = useCallback((email: string, password: string) => api.login(email, password), [])

  const verifyOtp = useCallback(async (email: string, code: string) => {
    const tokens = await api.verifyOtp(email, code)
    applySession(tokens)
  }, [])

  const logout = useCallback(async () => {
    const refreshToken = getStoredRefreshToken()
    applySession(null)
    if (refreshToken) {
      try {
        await api.logout(refreshToken)
      } catch {
        // already logged out locally, nothing else to do
      }
    }
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ user, loading, login, verifyOtp, logout }),
    [user, loading, login, verifyOtp, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
