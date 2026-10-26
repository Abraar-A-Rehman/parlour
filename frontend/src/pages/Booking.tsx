import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../api/api'
import { errorMessage } from '../api/client'
import SlotPicker from '../components/SlotPicker'
import { formatDateTime, formatDuration, formatPrice, isValidPhone, normalizePhone } from '../utils/format'
import { useSiteSettings } from '../hooks/useSiteSettings'
import type { BookingConfirmation, ServiceItem } from '../types'

export default function Booking() {
  const { settings } = useSiteSettings()
  const [params] = useSearchParams()
  const preselected = Number(params.get('service'))

  const [services, setServices] = useState<ServiceItem[]>([])
  const [serviceId, setServiceId] = useState<number | null>(Number.isFinite(preselected) && preselected > 0 ? preselected : null)
  const [date, setDate] = useState('')
  const [time, setTime] = useState('')
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [otp, setOtp] = useState('')
  const [otpSent, setOtpSent] = useState(false)
  const [devOtp, setDevOtp] = useState<string | null>(null)
  const [cooldown, setCooldown] = useState(0)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState<BookingConfirmation | null>(null)

  useEffect(() => {
    api
      .services()
      .then(setServices)
      .catch((e: unknown) => setError(errorMessage(e)))
  }, [])

  useEffect(() => {
    if (cooldown <= 0) return
    const t = window.setTimeout(() => setCooldown((c) => c - 1), 1000)
    return () => window.clearTimeout(t)
  }, [cooldown])

  const selected = services.find((s) => s.id === serviceId) ?? null
  const slotChosen = serviceId !== null && date !== '' && time !== ''
  const detailsValid = name.trim().length >= 2 && isValidPhone(phone)

  const resetSlot = () => {
    setTime('')
    setOtpSent(false)
    setOtp('')
  }

  const sendCode = async () => {
    setBusy(true)
    setError(null)
    try {
      const res = await api.requestCustomerOtp(normalizePhone(phone))
      setOtpSent(true)
      setDevOtp(res.devOtp)
      setCooldown(30)
    } catch (e: unknown) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  const confirm = async (e: FormEvent) => {
    e.preventDefault()
    if (serviceId === null) return
    setBusy(true)
    setError(null)
    try {
      const result = await api.book({
        customerName: name.trim(),
        customerPhone: normalizePhone(phone),
        serviceId,
        startTime: `${date}T${time}:00`,
        otp: otp.trim(),
      })
      setDone(result)
    } catch (err: unknown) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  if (done) {
    return (
      <section className="section narrow center">
        <div className="success-icon" aria-hidden="true">
          ✓
        </div>
        <h2>Booking received!</h2>
        <p>
          <strong>{done.serviceName}</strong>
          <br />
          {formatDateTime(done.startTime)}
        </p>
        <p className="muted">We have texted you the details. We will confirm your appointment shortly.</p>
        <Link to="/" className="btn btn-primary">
          Back to home
        </Link>
      </section>
    )
  }

  return (
    <section className="section narrow">
      <h2>Book your appointment</h2>
      {settings && (
        <p className="call-box">
          Prefer to talk to someone? <a href={`tel:${settings.phone.replace(/[\s-]/g, '')}`}>📞 Call {settings.phone}</a> and we
          will book it for you.
        </p>
      )}

      <div className="step">
        <h3>1. Choose a service</h3>
        <select
          value={serviceId ?? ''}
          onChange={(e) => {
            setServiceId(e.target.value ? Number(e.target.value) : null)
            resetSlot()
          }}
          aria-label="Service"
        >
          <option value="">Select a service…</option>
          {services.map((s) => (
            <option key={s.id} value={s.id}>
              {s.name} · {formatPrice(s.price)} · {formatDuration(s.durationMin)}
            </option>
          ))}
        </select>
      </div>

      {serviceId !== null && (
        <div className="step">
          <h3>2. Pick a date and time</h3>
          <SlotPicker
            serviceId={serviceId}
            date={date}
            time={time}
            onDate={(d) => {
              setDate(d)
              resetSlot()
            }}
            onTime={(t) => {
              setTime(t)
              setOtpSent(false)
              setOtp('')
            }}
          />
        </div>
      )}

      {slotChosen && (
        <form className="step" onSubmit={confirm}>
          <h3>3. Your details</h3>
          <label>
            Full name
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              autoComplete="name"
              maxLength={100}
              required
            />
          </label>
          <label>
            Mobile number
            <input
              value={phone}
              onChange={(e) => {
                setPhone(e.target.value)
                setOtpSent(false)
              }}
              inputMode="tel"
              autoComplete="tel"
              placeholder="+8801XXXXXXXXX"
              required
            />
          </label>
          {phone !== '' && !isValidPhone(phone) && <p className="error">Enter a valid mobile number.</p>}

          {!otpSent ? (
            <button type="button" className="btn btn-primary" disabled={!detailsValid || busy} onClick={sendCode}>
              {busy ? 'Sending…' : 'Send verification code'}
            </button>
          ) : (
            <>
              <label>
                6-digit code sent to {phone}
                <input
                  value={otp}
                  onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  maxLength={6}
                  required
                />
              </label>
              {devOtp && (
                <p className="dev-note">
                  Development mode: no SMS gateway is connected, your code is <strong>{devOtp}</strong>
                </p>
              )}
              <div className="row">
                <button type="submit" className="btn btn-primary" disabled={otp.length !== 6 || busy}>
                  {busy ? 'Confirming…' : 'Confirm booking'}
                </button>
                <button type="button" className="btn btn-link" disabled={cooldown > 0 || busy} onClick={sendCode}>
                  {cooldown > 0 ? `Resend in ${cooldown}s` : 'Resend code'}
                </button>
              </div>
            </>
          )}
          {selected && (
            <p className="muted">
              {selected.name} · {formatPrice(selected.price)} · {formatDuration(selected.durationMin)}
            </p>
          )}
        </form>
      )}

      {error && <p className="error">{error}</p>}
    </section>
  )
}
