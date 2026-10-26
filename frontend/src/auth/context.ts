import { createContext } from 'react'
import type { LoginChallenge, SessionUser } from '../types'

export interface AuthContextValue {
  user: SessionUser | null
  /** true while the stored refresh token is being checked after a page reload */
  loading: boolean
  /** Step 1: email + password. The server texts an OTP to the admin's phone. */
  login: (email: string, password: string) => Promise<LoginChallenge>
  /** Step 2: OTP. Completes the login. */
  verifyOtp: (email: string, code: string) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
