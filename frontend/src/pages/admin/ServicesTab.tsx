import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api/api'
import { errorMessage } from '../../api/client'
import { formatDuration, formatPrice } from '../../utils/format'
import type { ServiceInput, ServiceItem } from '../../types'
import ImageField from './ImageField'

const EMPTY: ServiceInput = {
  name: '',
  category: 'Hair',
  description: '',
  price: 0,
  durationMin: 30,
  imageUrl: '',
  active: true,
}

function toInput(s: ServiceItem): ServiceInput {
  return {
    name: s.name,
    category: s.category,
    description: s.description ?? '',
    price: s.price,
    durationMin: s.durationMin,
    imageUrl: s.imageUrl ?? '',
    active: s.active,
  }
}

/** OWNER only: add, edit and remove services (name, price, duration, picture ...). */
export default function ServicesTab() {
  const [services, setServices] = useState<ServiceItem[]>([])
  const [form, setForm] = useState<ServiceInput | null>(null)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      setServices(await api.adminServices())
    } catch (e: unknown) {
      setError(errorMessage(e))
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  const set = <K extends keyof ServiceInput>(key: K, value: ServiceInput[K]) =>
    setForm((f) => (f ? { ...f, [key]: value } : f))

  const save = async (e: FormEvent) => {
    e.preventDefault()
    if (!form) return
    setBusy(true)
    setError(null)
    try {
      if (editingId === null) {
        await api.createService(form)
      } else {
        await api.updateService(editingId, form)
      }
      setForm(null)
      setEditingId(null)
      await load()
    } catch (err: unknown) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const remove = async (s: ServiceItem) => {
    if (!window.confirm(`Delete "${s.name}"? Services that already have bookings are hidden instead.`)) return
    setError(null)
    try {
      await api.deleteService(s.id)
      await load()
    } catch (err: unknown) {
      setError(errorMessage(err))
    }
  }

  return (
    <div>
      <div className="toolbar">
        <button
          type="button"
          className="btn btn-primary btn-small"
          onClick={() => {
            setForm(EMPTY)
            setEditingId(null)
          }}
        >
          + Add service
        </button>
      </div>

      {error && <p className="error">{error}</p>}

      {form && (
        <form className="panel" onSubmit={save}>
          <h3>{editingId === null ? 'New service' : 'Edit service'}</h3>
          <div className="two-col">
            <label>
              Name
              <input value={form.name} onChange={(e) => set('name', e.target.value)} maxLength={120} required />
            </label>
            <label>
              Category
              <input value={form.category} onChange={(e) => set('category', e.target.value)} maxLength={60} required />
            </label>
          </div>
          <label>
            Description
            <textarea value={form.description} onChange={(e) => set('description', e.target.value)} maxLength={1000} rows={3} />
          </label>
          <div className="two-col">
            <label>
              Price (৳)
              <input
                type="number"
                min={0}
                step="1"
                value={form.price}
                onChange={(e) => set('price', Number(e.target.value))}
                required
              />
            </label>
            <label>
              Duration (minutes)
              <input
                type="number"
                min={5}
                max={480}
                step={5}
                value={form.durationMin}
                onChange={(e) => set('durationMin', Number(e.target.value))}
                required
              />
            </label>
          </div>
          <ImageField label="Image" value={form.imageUrl} onChange={(url) => set('imageUrl', url)} />
          <label className="checkbox">
            <input type="checkbox" checked={form.active} onChange={(e) => set('active', e.target.checked)} />
            Show on the website
          </label>
          <div className="row">
            <button type="submit" className="btn btn-primary" disabled={busy}>
              {busy ? 'Saving…' : 'Save'}
            </button>
            <button
              type="button"
              className="btn btn-link"
              onClick={() => {
                setForm(null)
                setEditingId(null)
              }}
            >
              Cancel
            </button>
          </div>
        </form>
      )}

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Service</th>
              <th>Category</th>
              <th>Price</th>
              <th>Duration</th>
              <th>Visible</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {services.map((s) => (
              <tr key={s.id}>
                <td>{s.name}</td>
                <td>{s.category}</td>
                <td>{formatPrice(s.price)}</td>
                <td>{formatDuration(s.durationMin)}</td>
                <td>{s.active ? 'Yes' : <span className="badge status-cancelled">Hidden</span>}</td>
                <td className="actions">
                  <button
                    type="button"
                    className="btn btn-small btn-outline"
                    onClick={() => {
                      setForm(toInput(s))
                      setEditingId(s.id)
                      window.scrollTo({ top: 0, behavior: 'smooth' })
                    }}
                  >
                    Edit
                  </button>
                  <button type="button" className="btn btn-small btn-danger" onClick={() => void remove(s)}>
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
