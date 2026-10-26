import { useEffect, useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { errorMessage } from '../api/client'
import { useAuth } from '../auth/useAuth'

export default function AdminLogin() {
  const { user, loading, login, verifyOtp } = useAuth()
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [code, setCode] = useState('')
  const [maskedPhone, setMaskedPhone] = useState<string | null>(null)
  const [devOtp, setDevOtp] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (user) navigate('/admin', { replace: true })
  }, [user, navigate])

  if (!loading && user) {
    return <Navigate to="/admin" replace />
  }

  const submitCredentials = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const challenge = await login(email.trim(), password)
      setMaskedPhone(challenge.maskedPhone)
      setDevOtp(challenge.devOtp)
    } catch (err: unknown) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const submitCode = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await verifyOtp(email.trim(), code.trim())
      navigate('/admin', { replace: true })
    } catch (err: unknown) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const startOver = () => {
    setMaskedPhone(null)
    setDevOtp(null)
    setCode('')
    setPassword('')
    setError(null)
  }

  return (
    <section className="section narrow">
      <div className="auth-card">
        <h2>Staff login</h2>

        {maskedPhone === null ? (
          <form onSubmit={submitCredentials}>
            <p className="muted">Step 1 of 2: sign in with your email and password.</p>
            <label>
              Email
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="username" required />
            </label>
            <label>
              Password
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                required
              />
            </label>
            <button type="submit" className="btn btn-primary" disabled={busy}>
              {busy ? 'Checking…' : 'Continue'}
            </button>
          </form>
        ) : (
          <form onSubmit={submitCode}>
            <p className="muted">
              Step 2 of 2: we sent a 6-digit code to <strong>{maskedPhone}</strong>.
            </p>
            <label>
              Verification code
              <input
                value={code}
                onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                autoFocus
                required
              />
            </label>
            {devOtp && (
              <p className="dev-note">
                Development mode: no SMS gateway is connected, your code is <strong>{devOtp}</strong>
              </p>
            )}
            <div className="row">
              <button type="submit" className="btn btn-primary" disabled={code.length !== 6 || busy}>
                {busy ? 'Verifying…' : 'Verify and sign in'}
              </button>
              <button type="button" className="btn btn-link" onClick={startOver}>
                Start over
              </button>
            </div>
          </form>
        )}

        {error && <p className="error">{error}</p>}
      </div>
    </section>
  )
}
