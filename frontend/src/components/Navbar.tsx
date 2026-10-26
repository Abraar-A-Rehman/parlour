import { useState } from 'react'
import { Link, NavLink } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

interface Props {
  name: string
  phone: string | null
}

export default function Navbar({ name, phone }: Props) {
  const { user } = useAuth()
  const [open, setOpen] = useState(false)
  const close = () => setOpen(false)

  return (
    <header className="navbar">
      <div className="navbar-inner">
        <Link to="/" className="brand" onClick={close}>
          {name}
        </Link>

        <button
          type="button"
          className="menu-toggle"
          aria-label="Toggle menu"
          aria-expanded={open}
          onClick={() => setOpen((o) => !o)}
        >
          ☰
        </button>

        <nav className={open ? 'nav-links open' : 'nav-links'}>
          <Link to="/#services" onClick={close}>
            Services
          </Link>
          <Link to="/#about" onClick={close}>
            About
          </Link>
          <Link to="/#faq" onClick={close}>
            FAQ
          </Link>
          <Link to="/#visit" onClick={close}>
            Visit us
          </Link>
          {phone && (
            <a className="nav-phone" href={`tel:${phone.replace(/[\s-]/g, '')}`}>
              📞 {phone}
            </a>
          )}
          <NavLink to="/book" className="btn btn-primary btn-small" onClick={close}>
            Book now
          </NavLink>
          <NavLink to={user ? '/admin' : '/admin/login'} className="staff-link" onClick={close}>
            {user ? 'Dashboard' : 'Staff login'}
          </NavLink>
        </nav>
      </div>
    </header>
  )
}
