import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api/api'
import { errorMessage } from '../../api/client'
import { setSiteSettings, useSiteSettings } from '../../hooks/useSiteSettings'
import type { SiteSettings } from '../../types'
import ImageField from './ImageField'

/** OWNER only: edit the texts shown on the public website. */
export default function SiteTab() {
  const { settings } = useSiteSettings()
  const [form, setForm] = useState<SiteSettings | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    if (settings && form === null) {
      setForm(settings)
    }
  }, [settings, form])

  if (!form) {
    return <p className="muted">Loading…</p>
  }

  const set = <K extends keyof SiteSettings>(key: K, value: SiteSettings[K]) => {
    setSaved(false)
    setForm((f) => (f ? { ...f, [key]: value } : f))
  }

  const save = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const result = await api.updateSettings(form)
      setSiteSettings(result)
      setSaved(true)
    } catch (err: unknown) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="panel" onSubmit={save}>
      <h3>Website content</h3>
      <div className="two-col">
        <label>
          Parlour name
          <input value={form.parlourName} onChange={(e) => set('parlourName', e.target.value)} maxLength={120} required />
        </label>
        <label>
          Tagline
          <input value={form.tagline} onChange={(e) => set('tagline', e.target.value)} maxLength={200} required />
        </label>
      </div>
      <label>
        About us
        <textarea value={form.about ?? ''} onChange={(e) => set('about', e.target.value)} maxLength={2000} rows={4} />
      </label>
      <label>
        Special offer banner (leave empty to hide)
        <input value={form.offerText ?? ''} onChange={(e) => set('offerText', e.target.value)} maxLength={500} />
      </label>
      <div className="two-col">
        <label>
          Phone
          <input value={form.phone} onChange={(e) => set('phone', e.target.value)} inputMode="tel" required />
        </label>
        <label>
          Email
          <input type="email" value={form.email ?? ''} onChange={(e) => set('email', e.target.value)} maxLength={200} />
        </label>
      </div>
      <label>
        Address
        <input value={form.address ?? ''} onChange={(e) => set('address', e.target.value)} maxLength={500} />
      </label>
      <label>
        Opening hours
        <input value={form.openingHours ?? ''} onChange={(e) => set('openingHours', e.target.value)} maxLength={500} />
      </label>
      <ImageField label="Hero (top banner) image" value={form.heroImageUrl ?? ''} onChange={(url) => set('heroImageUrl', url)} />

      <button type="submit" className="btn btn-primary" disabled={busy}>
        {busy ? 'Saving…' : 'Save changes'}
      </button>
      {error && <p className="error">{error}</p>}
      {saved && <p className="success">Saved. The website now shows your changes.</p>}
    </form>
  )
}
