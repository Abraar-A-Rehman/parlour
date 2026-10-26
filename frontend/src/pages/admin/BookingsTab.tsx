import { useEffect, useMemo, useState } from 'react'
import { api } from '../../api/api'
import { errorMessage } from '../../api/client'
import { formatDateTime, toISODate } from '../../utils/format'
import type { Booking, BookingStatus } from '../../types'

const STATUS_FILTERS: Array<BookingStatus | 'ALL'> = ['ALL', 'PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED']

export default function BookingsTab() {
  const [bookings, setBookings] = useState<Booking[]>([])
  const [date, setDate] = useState('')
  const [statusFilter, setStatusFilter] = useState<BookingStatus | 'ALL'>('ALL')
  const [reloadKey, setReloadKey] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    api
      .adminBookings(date || undefined)
      .then((list) => {
        if (!cancelled) setBookings(list)
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
  }, [date, reloadKey])

  const visible = useMemo(
    () => bookings.filter((b) => statusFilter === 'ALL' || b.status === statusFilter),
    [bookings, statusFilter],
  )

  const changeStatus = async (id: number, status: BookingStatus) => {
    setError(null)
    try {
      const updated = await api.setBookingStatus(id, status)
      setBookings((list) => list.map((b) => (b.id === id ? updated : b)))
    } catch (e: unknown) {
      setError(errorMessage(e))
    }
  }

  return (
    <div>
      <div className="toolbar">
        <label className="inline">
          Date
          <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        </label>
        <button type="button" className="btn btn-outline btn-small" onClick={() => setDate(toISODate(new Date()))}>
          Today
        </button>
        <button type="button" className="btn btn-outline btn-small" onClick={() => setDate('')}>
          All dates
        </button>
        <label className="inline">
          Status
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value as BookingStatus | 'ALL')}>
            {STATUS_FILTERS.map((s) => (
              <option key={s} value={s}>
                {s === 'ALL' ? 'All' : s.charAt(0) + s.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
        </label>
        <button type="button" className="btn btn-outline btn-small" onClick={() => setReloadKey((k) => k + 1)}>
          ↻ Refresh
        </button>
      </div>

      {error && <p className="error">{error}</p>}
      {loading && <p className="muted">Loading bookings…</p>}
      {!loading && visible.length === 0 && <p className="muted">No bookings found.</p>}

      {visible.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>When</th>
                <th>Customer</th>
                <th>Phone</th>
                <th>Service</th>
                <th>Booked via</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {visible.map((b) => (
                <tr key={b.id}>
                  <td>{formatDateTime(b.startTime)}</td>
                  <td>
                    {b.customerName}
                    {b.notes && <div className="muted small">{b.notes}</div>}
                  </td>
                  <td>
                    <a href={`tel:${b.customerPhone}`}>{b.customerPhone}</a>
                  </td>
                  <td>{b.serviceName}</td>
                  <td>
                    <span className={b.bookedVia === 'CALL' ? 'badge call' : 'badge online'}>
                      {b.bookedVia === 'CALL' ? '📞 Phone call' : '🗓 Calendar'}
                    </span>
                  </td>
                  <td>
                    <span className={`badge status-${b.status.toLowerCase()}`}>{b.status}</span>
                  </td>
                  <td className="actions">
                    {b.status === 'PENDING' && (
                      <button type="button" className="btn btn-small btn-primary" onClick={() => void changeStatus(b.id, 'CONFIRMED')}>
                        Confirm
                      </button>
                    )}
                    {b.status === 'CONFIRMED' && (
                      <button type="button" className="btn btn-small btn-outline" onClick={() => void changeStatus(b.id, 'COMPLETED')}>
                        Complete
                      </button>
                    )}
                    {(b.status === 'PENDING' || b.status === 'CONFIRMED') && (
                      <button type="button" className="btn btn-small btn-danger" onClick={() => void changeStatus(b.id, 'CANCELLED')}>
                        Cancel
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
