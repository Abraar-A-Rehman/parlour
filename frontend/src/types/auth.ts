export type Role = 'OWNER' | 'MANAGER'

export interface LoginChallenge {
  otpRequired: boolean
  maskedPhone: string
  devOtp: string | null
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  role: Role
  email: string
  fullName: string
}

export interface SessionUser {
  email: string
  fullName: string
  role: Role
}
