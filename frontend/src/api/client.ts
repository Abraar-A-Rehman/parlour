import type { TokenResponse } from '../types'

/**
 * Tiny fetch wrapper.
 * - Access token (15 min) lives only in memory.
 * - Refresh token (7 days) lives in sessionStorage so a page reload keeps you logged in.
 * - When the API answers 401, the client silently asks /api/auth/refresh for a new pair and retries once.
 */

const REFRESH_KEY = 'parlour.refreshToken'

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
  formData?: FormData
  auth?: boolean
}

type SessionListener = (tokens: TokenResponse | null) => void

let accessToken: string | null = null
let refreshInFlight: Promise<boolean> | null = null
const listeners = new Set<SessionListener>()

export function subscribeSession(listener: SessionListener): () => void {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

export function applySession(tokens: TokenResponse | null): void {
  accessToken = tokens ? tokens.accessToken : null
  if (tokens) {
    sessionStorage.setItem(REFRESH_KEY, tokens.refreshToken)
  } else {
    sessionStorage.removeItem(REFRESH_KEY)
  }
  listeners.forEach((l) => l(tokens))
}

export function getStoredRefreshToken(): string | null {
  return sessionStorage.getItem(REFRESH_KEY)
}

function hasMessage(value: unknown): value is { message: string } {
  return (
    typeof value === 'object' &&
    value !== null &&
    'message' in value &&
    typeof (value as { message: unknown }).message === 'string'
  )
}

async function toApiError(res: Response): Promise<ApiError> {
  let message = `Request failed (${res.status})`
  try {
    const data: unknown = await res.json()
    if (hasMessage(data)) {
      message = data.message
    }
  } catch {
    // body was not JSON, keep the generic message
  }
  return new ApiError(res.status, message)
}

function send(path: string, opts: RequestOptions): Promise<Response> {
  const headers: Record<string, string> = {}
  let body: BodyInit | undefined
  if (opts.formData) {
    body = opts.formData
  } else if (opts.body !== undefined) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(opts.body)
  }
  if (opts.auth && accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`
  }
  return fetch(path, { method: opts.method ?? 'GET', headers, body })
}

async function readJson<T>(res: Response): Promise<T> {
  if (!res.ok) {
    throw await toApiError(res)
  }
  if (res.status === 204) {
    return undefined as T
  }
  const data: unknown = await res.json()
  return data as T
}

/** Uses the stored refresh token to get a new token pair. Many callers share one request. */
export function refreshSession(): Promise<boolean> {
  if (refreshInFlight) {
    return refreshInFlight
  }
  const stored = getStoredRefreshToken()
  if (!stored) {
    return Promise.resolve(false)
  }
  refreshInFlight = (async () => {
    try {
      const res = await fetch('/api/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: stored }),
      })
      if (!res.ok) {
        applySession(null)
        return false
      }
      const data: unknown = await res.json()
      applySession(data as TokenResponse)
      return true
    } catch {
      return false
    } finally {
      refreshInFlight = null
    }
  })()
  return refreshInFlight
}

export async function request<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const res = await send(path, opts)
  if (res.status === 401 && opts.auth && (await refreshSession())) {
    return readJson<T>(await send(path, opts))
  }
  return readJson<T>(res)
}

export function errorMessage(e: unknown): string {
  return e instanceof Error ? e.message : 'Something went wrong. Please try again.'
}
