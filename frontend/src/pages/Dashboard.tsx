import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import BookingsTab from './admin/BookingsTab'
import PhoneBookingTab from './admin/PhoneBookingTab'
import ServicesTab from './admin/ServicesTab'
import SiteTab from './admin/SiteTab'

type Tab = 'bookings' | 'phone' | 'services' | 'site'

const TAB_LABELS: Record<Tab, string> = {
  bookings: 'Bookings',
  phone: 'New phone booking',
  services: 'Services',
  site: 'Website content',
}

export default function Dashboard() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [tab, setTab] = useState<Tab>('bookings')

  if (!user) {
    return null // RequireRole already redirects; this keeps TypeScript happy
  }

  const isOwner = user.role === 'OWNER'
  // Everyone sees bookings. Only the owner may edit the website (the server enforces this too).
  const tabs: Tab[] = isOwner ? ['bookings', 'phone', 'services', 'site'] : ['bookings', 'phone']

  const signOut = async () => {
    await logout()
    navigate('/', { replace: true })
  }

  return (
    <section className="section wide">
      <div className="dash-head">
        <div>
          <h2>Dashboard</h2>
          <p className="muted">
            {user.fullName} · <span className="badge online">{user.role}</span>
          </p>
        </div>
        <button type="button" className="btn btn-outline btn-small" onClick={() => void signOut()}>
          Sign out
        </button>
      </div>

      <div className="tabs" role="tablist">
        {tabs.map((t) => (
          <button
            key={t}
            type="button"
            role="tab"
            aria-selected={tab === t}
            className={tab === t ? 'tab active' : 'tab'}
            onClick={() => setTab(t)}
          >
            {TAB_LABELS[t]}
          </button>
        ))}
      </div>

      {tab === 'bookings' && <BookingsTab />}
      {tab === 'phone' && <PhoneBookingTab />}
      {tab === 'services' && isOwner && <ServicesTab />}
      {tab === 'site' && isOwner && <SiteTab />}
    </section>
  )
}
