import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { api } from '../api/api'
import { errorMessage } from '../api/client'
import ServiceCard from '../components/ServiceCard'
import { useSiteSettings } from '../hooks/useSiteSettings'
import type { ServiceItem } from '../types'

const FAQ: Array<{ q: string; a: string }> = [
  {
    q: 'How do I book an appointment?',
    a: 'Choose a service, pick a date and time on the calendar, enter your phone number and confirm with the 6-digit code we text you. You can also simply call us.',
  },
  {
    q: 'Why do I need to verify my phone number?',
    a: 'The code proves the number is yours, so nobody can book in your name and we can reach you if anything changes.',
  },
  {
    q: 'Can I cancel or change my appointment?',
    a: 'Yes. Please call us at least a few hours before your slot and we will move or cancel it for you.',
  },
  {
    q: 'Is my personal information safe?',
    a: 'Only our owner and manager can see bookings, and they sign in with a password plus a one-time code sent to their phone.',
  },
  {
    q: 'Do you take walk-ins?',
    a: 'We try to, but booking ahead guarantees your preferred time.',
  },
]

const WHY: Array<{ icon: string; title: string; text: string }> = [
  { icon: '✨', title: 'Experienced stylists', text: 'Trained professionals who listen before they cut, colour or style.' },
  { icon: '🧴', title: 'Trusted products', text: 'Gentle, quality products suited to local hair and skin.' },
  { icon: '🕒', title: 'Easy booking', text: 'See real free times, book in a minute, or just call us.' },
]

export default function Home() {
  const { settings } = useSiteSettings()
  const location = useLocation()
  const [services, setServices] = useState<ServiceItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api
      .services()
      .then(setServices)
      .catch((e: unknown) => setError(errorMessage(e)))
      .finally(() => setLoading(false))
  }, [])

  // Scroll to #services, #about ... when arriving from the navbar
  useEffect(() => {
    if (!location.hash) {
      window.scrollTo({ top: 0 })
      return
    }
    const el = document.getElementById(location.hash.slice(1))
    el?.scrollIntoView({ behavior: 'smooth' })
  }, [location.hash, loading])

  const grouped = useMemo(() => {
    const map = new Map<string, ServiceItem[]>()
    for (const s of services) {
      map.set(s.category, [...(map.get(s.category) ?? []), s])
    }
    return [...map.entries()]
  }, [services])

  const gallery = services.filter((s) => s.imageUrl).slice(0, 6)
  const phoneLink = settings ? `tel:${settings.phone.replace(/[\s-]/g, '')}` : '#'

  return (
    <>
      <section
        className="hero"
        style={settings?.heroImageUrl ? { backgroundImage: `linear-gradient(rgba(40,20,30,.55), rgba(40,20,30,.55)), url(${settings.heroImageUrl})` } : undefined}
      >
        <div className="hero-inner">
          <p className="eyebrow">Beauty parlour</p>
          <h1>{settings?.parlourName ?? 'Welcome'}</h1>
          <p className="hero-tagline">{settings?.tagline}</p>
          <div className="hero-actions">
            <Link to="/book" className="btn btn-primary">
              Book appointment
            </Link>
            {settings && (
              <a href={phoneLink} className="btn btn-light">
                📞 Call {settings.phone}
              </a>
            )}
          </div>
        </div>
      </section>

      {settings?.offerText && <div className="offer-bar">🎁 {settings.offerText}</div>}

      <section id="services" className="section">
        <h2>Our services</h2>
        <p className="section-sub">Clear prices and durations, so there are no surprises.</p>
        {loading && <p className="muted">Loading services…</p>}
        {error && <p className="error">{error}</p>}
        {grouped.map(([category, items]) => (
          <div key={category} className="category">
            <h3 className="category-title">{category}</h3>
            <div className="card-grid">
              {items.map((s) => (
                <ServiceCard key={s.id} service={s} />
              ))}
            </div>
          </div>
        ))}
        <div className="center">
          <Link to="/book" className="btn btn-primary">
            Book appointment
          </Link>
        </div>
      </section>

      <section className="section alt">
        <h2>Why choose us</h2>
        <div className="why-grid">
          {WHY.map((w) => (
            <div key={w.title} className="why-card">
              <span className="why-icon" aria-hidden="true">
                {w.icon}
              </span>
              <h3>{w.title}</h3>
              <p className="muted">{w.text}</p>
            </div>
          ))}
        </div>
      </section>

      {gallery.length > 0 && (
        <section className="section">
          <h2>Gallery</h2>
          <div className="gallery">
            {gallery.map((s) => (
              <img key={s.id} src={s.imageUrl ?? ''} alt={s.name} loading="lazy" />
            ))}
          </div>
        </section>
      )}

      <section id="about" className="section alt">
        <h2>About us</h2>
        <p className="about-text">{settings?.about}</p>
      </section>

      <section id="faq" className="section">
        <h2>Questions &amp; answers</h2>
        <div className="faq">
          {FAQ.map((item) => (
            <details key={item.q}>
              <summary>{item.q}</summary>
              <p className="muted">{item.a}</p>
            </details>
          ))}
        </div>
      </section>

      <section id="visit" className="section alt">
        <h2>Visit us</h2>
        {settings && (
          <div className="visit-grid">
            <div>
              <h4>Address</h4>
              <p>{settings.address}</p>
              {settings.address && (
                <a
                  href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(settings.address)}`}
                  target="_blank"
                  rel="noreferrer noopener"
                >
                  Open in Google Maps ↗
                </a>
              )}
            </div>
            <div>
              <h4>Opening hours</h4>
              <p>{settings.openingHours}</p>
            </div>
            <div>
              <h4>Contact</h4>
              <p>
                <a href={phoneLink}>{settings.phone}</a>
              </p>
              {settings.email && (
                <p>
                  <a href={`mailto:${settings.email}`}>{settings.email}</a>
                </p>
              )}
            </div>
          </div>
        )}
        <div className="center">
          <Link to="/book" className="btn btn-primary">
            Book appointment
          </Link>
        </div>
      </section>
    </>
  )
}
