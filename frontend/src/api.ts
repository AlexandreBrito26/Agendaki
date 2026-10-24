const BASE = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '')

export interface ServiceItem { id: number; name: string; durationMin: number; priceCents: number }
export interface ProfessionalItem { id: number; name: string; serviceIds: number[] }
export interface Catalog {
  businessName: string
  slug: string
  timezone: string
  services: ServiceItem[]
  professionals: ProfessionalItem[]
}
export interface Availability { date: string; slots: string[] }
export interface Appointment {
  id: number
  serviceId: number
  serviceName: string
  professionalId: number
  professionalName: string
  customerName: string
  customerPhone: string
  date: string
  startTime: string
  endTime: string
  status: 'CONFIRMED' | 'CANCELLED'
  startsAt?: string
  endsAt?: string
  manageToken?: string
}
export interface BookingPayload {
  serviceId: number
  professionalId: number
  date: string
  time: string
  customerName: string
  customerPhone: string
  customerEmail?: string
}

export class ApiError extends Error {
  status: number
  fields?: Record<string, string>
  constructor(message: string, status: number, fields?: Record<string, string>) {
    super(message)
    this.status = status
    this.fields = fields
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response
  try {
    res = await fetch(`${BASE}/api/public${path}`, {
      ...init,
      headers: { 'Content-Type': 'application/json', ...init?.headers },
    })
  } catch {
    throw new ApiError('Não foi possível conectar. Verifique sua internet e tente de novo.', 0)
  }
  if (!res.ok) {
    let body: { message?: string; fields?: Record<string, string> } = {}
    try { body = await res.json() } catch { /* corpo vazio */ }
    const fallback = res.status === 404 ? 'Não encontramos o que você procura.' : 'Algo deu errado. Tente novamente.'
    throw new ApiError(body.message ?? fallback, res.status, body.fields)
  }
  return res.json() as Promise<T>
}

const enc = encodeURIComponent

export const api = {
  catalog: (slug: string) => request<Catalog>(`/businesses/${enc(slug)}`),
  availability: (slug: string, serviceId: number, professionalId: number, date: string) =>
    request<Availability>(
      `/businesses/${enc(slug)}/availability?serviceId=${serviceId}&professionalId=${professionalId}&date=${date}`),
  book: (slug: string, payload: BookingPayload) =>
    request<Appointment>(`/businesses/${enc(slug)}/appointments`, { method: 'POST', body: JSON.stringify(payload) }),
  get: (token: string) => request<Appointment>(`/appointments/${enc(token)}`),
  cancel: (token: string) => request<Appointment>(`/appointments/${enc(token)}/cancel`, { method: 'POST' }),
  reschedule: (token: string, date: string, time: string) =>
    request<Appointment>(`/appointments/${enc(token)}/reschedule`, { method: 'POST', body: JSON.stringify({ date, time }) }),
}
