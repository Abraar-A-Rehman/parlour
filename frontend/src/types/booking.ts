export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED'

export type BookedVia = 'CALENDAR' | 'CALL'

export interface OtpSent {
  sent: boolean
  devOtp: string | null
}

export interface BookingConfirmation {
  id: number
  serviceName: string
  startTime: string
  endTime: string
  status: BookingStatus
}

export interface Booking {
  id: number
  customerName: string
  customerPhone: string
  serviceId: number
  serviceName: string
  startTime: string
  endTime: string
  status: BookingStatus
  bookedVia: BookedVia
  notes: string | null
}

export interface PublicBookingInput {
  customerName: string
  customerPhone: string
  serviceId: number
  startTime: string
  otp: string
}

export interface PhoneBookingInput {
  customerName: string
  customerPhone: string
  serviceId: number
  startTime: string
  notes: string
}
