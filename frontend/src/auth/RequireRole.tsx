import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import type { Role } from '../types'
import { useAuth } from './useAuth'

interface Props {
  roles: Role[]
  children: ReactNode
}

/** Route guard. (The real protection is on the server; this only avoids showing pages you cannot use.) */
export default function RequireRole({ roles, children }: Props) {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return <p className="center-note">Loading…</p>
  }
  if (!user) {
    return <Navigate to="/admin/login" replace state={{ from: location.pathname }} />
  }
  if (!roles.includes(user.role)) {
    return <Navigate to="/admin" replace />
  }
  return <>{children}</>
}
