export interface ServiceItem {
  id: number
  name: string
  category: string
  description: string | null
  price: number
  durationMin: number
  imageUrl: string | null
  active: boolean
}

export interface ServiceInput {
  name: string
  category: string
  description: string
  price: number
  durationMin: number
  imageUrl: string
  active: boolean
}

export interface SiteSettings {
  parlourName: string
  tagline: string
  about: string | null
  phone: string
  email: string | null
  address: string | null
  openingHours: string | null
  offerText: string | null
  heroImageUrl: string | null
}
