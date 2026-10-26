import { useEffect, useState } from 'react'
import { api } from '../api/api'
import { errorMessage } from '../api/client'
import { formatLongDate, formatTime } from '../utils/format'
import Calendar from './Calendar'

interface Props {
  serviceId: number | null
  date: string
  time: string
  onDate: (isoDate: string) => void
  onTime: (hhmm: string) => void
}

/** Calendar + the free time slots for the chosen service and date (loaded from the API). */
export default function SlotPicker({ serviceId, date, time, onDate, onTime }: Props) {
  const [slots, setSlots] = useState<string[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (serviceId === null || !date) {
      setSlots([])
      return
    }
    let cancelled = false
    setLoading(true)
    setError(null)
    api
      .availability(date, serviceId)
      .then((result) => {
        if (!cancelled) setSlots(result)
      })
      .catch((e: unknown) => {
        if (!cancelled) setError(errorMessage(e))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [serviceId, date])

  return (
    <div className="slot-picker">
      <Calendar value={date} onChange={onDate} />

      <div className="slots">
        {!date && <p className="muted">Pick a date to see the free times.</p>}
        {date && <h4>{formatLongDate(date)}</h4>}
        {loading && <p className="muted">Checking availability…</p>}
        {error && <p className="error">{error}</p>}
        {date && !loading && !error && slots.length === 0 && (
          <p className="muted">No free times on this day. Please try another date.</p>
        )}
        <div className="slot-list">
          {slots.map((s) => (
            <button
              key={s}
              type="button"
              className={s === time ? 'slot selected' : 'slot'}
              aria-pressed={s === time}
              onClick={() => onTime(s)}
            >
              {formatTime(s)}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
