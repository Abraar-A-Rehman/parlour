import { useState } from 'react'
import { parseISODate, toISODate } from '../utils/format'

interface Props {
  /** selected date as YYYY-MM-DD, or '' */
  value: string
  onChange: (isoDate: string) => void
  /** how far ahead customers may book */
  maxDaysAhead?: number
}

const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

function startOfDay(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate())
}

function monthIndex(d: Date): number {
  return d.getFullYear() * 12 + d.getMonth()
}

export default function Calendar({ value, onChange, maxDaysAhead = 90 }: Props) {
  const today = startOfDay(new Date())
  const lastDay = new Date(today.getFullYear(), today.getMonth(), today.getDate() + maxDaysAhead)

  const [cursor, setCursor] = useState<Date>(() => {
    const base = value ? parseISODate(value) : today
    return new Date(base.getFullYear(), base.getMonth(), 1)
  })

  const daysInMonth = new Date(cursor.getFullYear(), cursor.getMonth() + 1, 0).getDate()
  const leadingBlanks = cursor.getDay()

  const cells: Array<Date | null> = []
  for (let i = 0; i < leadingBlanks; i++) cells.push(null)
  for (let d = 1; d <= daysInMonth; d++) cells.push(new Date(cursor.getFullYear(), cursor.getMonth(), d))

  const canGoBack = monthIndex(cursor) > monthIndex(today)
  const canGoForward = monthIndex(cursor) < monthIndex(lastDay)

  const move = (delta: number) => setCursor(new Date(cursor.getFullYear(), cursor.getMonth() + delta, 1))

  return (
    <div className="calendar" role="group" aria-label="Choose a date">
      <div className="calendar-head">
        <button type="button" className="icon-btn" onClick={() => move(-1)} disabled={!canGoBack} aria-label="Previous month">
          ‹
        </button>
        <strong>{cursor.toLocaleDateString('en-GB', { month: 'long', year: 'numeric' })}</strong>
        <button type="button" className="icon-btn" onClick={() => move(1)} disabled={!canGoForward} aria-label="Next month">
          ›
        </button>
      </div>

      <div className="calendar-grid">
        {WEEKDAYS.map((w) => (
          <span key={w} className="calendar-weekday">
            {w}
          </span>
        ))}
        {cells.map((date, i) => {
          if (!date) return <span key={`blank-${i}`} />
          const iso = toISODate(date)
          const disabled = date < today || date > lastDay
          const selected = iso === value
          const classes = ['calendar-day', selected ? 'selected' : '', iso === toISODate(today) ? 'today' : '']
            .filter(Boolean)
            .join(' ')
          return (
            <button
              key={iso}
              type="button"
              className={classes}
              disabled={disabled}
              aria-pressed={selected}
              onClick={() => onChange(iso)}
            >
              {date.getDate()}
            </button>
          )
        })}
      </div>
    </div>
  )
}
