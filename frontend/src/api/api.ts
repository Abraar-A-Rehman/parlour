import { request } from './client'
import type {
  Booking,
  BookingConfirmation,
  BookingStatus,
  LoginChallenge,
  OtpSent,
  PhoneBookingInput,
  PublicBookingInput,
  ServiceInput,
  ServiceItem,
  SiteSettings,
  TokenResponse,
} from '../types'

export const api = {
  // ---- public ----
  services: () => request<ServiceItem[]>('/api/services'),
  settings: () => request<SiteSettings>('/api/settings'),
  availability: (date: string, serviceId: number) =>
    request<string[]>(`/api/bookings/availability?date=${encodeURIComponent(date)}&serviceId=${serviceId}`),
  requestCustomerOtp: (phone: string) =>
    request<OtpSent>('/api/bookings/request-otp', { method: 'POST', body: { phone } }),
  book: (input: PublicBookingInput) =>
    request<BookingConfirmation>('/api/bookings', { method: 'POST', body: input }),

  // ---- admin login ----
  login: (email: string, password: string) =>
    request<LoginChallenge>('/api/auth/login', { method: 'POST', body: { email, password } }),
  verifyOtp: (email: string, code: string) =>
    request<TokenResponse>('/api/auth/verify-otp', { method: 'POST', body: { email, code } }),
  logout: (refreshToken: string) =>
    request<void>('/api/auth/logout', { method: 'POST', body: { refreshToken } }),

  // ---- owner + manager ----
  adminBookings: (date?: string) =>
    request<Booking[]>(`/api/admin/bookings${date ? `?date=${encodeURIComponent(date)}` : ''}`, { auth: true }),
  createPhoneBooking: (input: PhoneBookingInput) =>
    request<Booking>('/api/admin/bookings', { method: 'POST', body: input, auth: true }),
  setBookingStatus: (id: number, status: BookingStatus) =>
    request<Booking>(`/api/admin/bookings/${id}/status`, { method: 'PATCH', body: { status }, auth: true }),
  adminServices: () => request<ServiceItem[]>('/api/admin/services', { auth: true }),

  // ---- owner only ----
  createService: (input: ServiceInput) =>
    request<ServiceItem>('/api/services', { method: 'POST', body: input, auth: true }),
  updateService: (id: number, input: ServiceInput) =>
    request<ServiceItem>(`/api/services/${id}`, { method: 'PUT', body: input, auth: true }),
  deleteService: (id: number) => request<void>(`/api/services/${id}`, { method: 'DELETE', auth: true }),
  updateSettings: (input: SiteSettings) =>
    request<SiteSettings>('/api/settings', { method: 'PUT', body: input, auth: true }),
  upload: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request<{ url: string }>('/api/uploads', { method: 'POST', formData, auth: true })
  },
}
