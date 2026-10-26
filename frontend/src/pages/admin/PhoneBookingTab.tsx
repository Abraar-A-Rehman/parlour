import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api/api'
import { errorMessage } from '../../api/client'
import SlotPicker from '../../components/SlotPicker'
import { formatDateTime, formatDuration, formatPrice, isValidPhone, normalizePhone } from '../../utils/format'
import type { ServiceItem } from '../../types'

/** For customers who phone the parlour: staff enter the booking by hand. */
export default function PhoneBookingTab() {
  const [services, setServices] = useState<ServiceItem[]>([])
  const [serviceId, setServiceId] = useState<number | null>(null)
  const [date, setDate] = useState('')
  const [time, setTime] = useState('')
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [notes, setNotes] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  // Incremented after each booking so the slot list is requested again (the taken slot disappears).
  const [slotRefresh, setSlotRefresh] = useState(0)

  useEffect(() => {
    api
      .services()
      .then(setServices)
      .catch((e: unknown) => setError(errorMessage(e)))
  }, [])

  const canSubmit = serviceId !== null && date !== '' && time !== '' && name.trim().length >= 2 && isValidPhone(phone)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    if (serviceId === null) return
    setBusy(true)
    setError(null)
    setSuccess(null)
    try {
      const created = await api.createPhoneBooking({
        customerName: name.trim(),
        customerPhone: normalizePhone(phone),
        serviceId,
        startTime: `${date}T${time}:00`,
        notes: notes.trim(),
      })
      setSuccess(`Saved: ${created.customerName}, ${created.serviceName}, ${formatDateTime(created.startTime)}`)
      setTime('')
      setName('')
      setPhone('')
      setNotes('')
      setSlotRefresh((n) => n + 1)
    } catch (err: unknown) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="panel" onSubmit={submit}>
      <h3>New booking taken by phone</h3>

      <label>
        Service
        <select
          value={serviceId ?? ''}
          onChange={(e) => {
            setServiceId(e.target.value ? Number(e.target.value) : null)
            setTime('')
          }}
          required
        >
          <option value="">Select a service…</option>
          {services.map((s) => (
            <option key={s.id} value={s.id}>
              {s.name} · {formatPrice(s.price)} · {formatDuration(s.durationMin)}
            </option>
          ))}
        </select>
      </label>

      {serviceId !== null && (
        <SlotPicker
          key={slotRefresh}
          serviceId={serviceId}
          date={date}
          time={time}
          onDate={(d) => {
            setDate(d)
            setTime('')
          }}
          onTime={setTime}
        />
      )}

      <div className="two-col">
        <label>
          Customer name
          <input value={name} onChange={(e) => setName(e.target.value)} maxLength={100} required />
        </label>
        <label>
          Customer phone
          <input value={phone} onChange={(e) => setPhone(e.target.value)} inputMode="tel" placeholder="01XXXXXXXXX" required />
        </label>
      </div>
      <label>
        Notes (optional)
        <input value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={500} />
      </label>

      <button type="submit" className="btn btn-primary" disabled={!canSubmit || busy}>
        {busy ? 'Saving…' : 'Save booking'}
      </button>

      {error && <p className="error">{error}</p>}
      {success && <p className="success">{success}</p>}
    </form>
  )
}
