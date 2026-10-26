import { useEffect } from 'react'
import { Route, Routes } from 'react-router-dom'
import RequireRole from './auth/RequireRole'
import Footer from './components/Footer'
import Navbar from './components/Navbar'
import AdminLogin from './pages/AdminLogin'
import Booking from './pages/Booking'
import Dashboard from './pages/Dashboard'
import Home from './pages/Home'
import NotFound from './pages/NotFound'
import { useSiteSettings } from './hooks/useSiteSettings'

export default function App() {
  const { settings } = useSiteSettings()

  useEffect(() => {
    if (settings) {
      document.title = settings.parlourName
    }
  }, [settings])

  return (
    <>
      <Navbar name={settings?.parlourName ?? 'Parlour'} phone={settings?.phone ?? null} />
      <main>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/book" element={<Booking />} />
          <Route path="/admin/login" element={<AdminLogin />} />
          <Route
            path="/admin"
            element={
              <RequireRole roles={['OWNER', 'MANAGER']}>
                <Dashboard />
              </RequireRole>
            }
          />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </main>
      <Footer name={settings?.parlourName ?? 'Parlour'} />
    </>
  )
}
