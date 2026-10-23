import { ApiError, type Appointment } from './api'

const BASE = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '')
const KEY = 'agendaki.session'

export interface Session { token: string; businessId: number; businessName: string; slug: string }
export interface Service { id: number; name: string; durationMin: number; priceCents: number; active: boolean }
export interface WorkingHours { weekday: number; startTime: string; endTime: string }
export interface Professional { id: number; name: string; active: boolean; serviceIds: number[]; workingHours: WorkingHours[] }

export const loadSession = (): Session | null => {
  try { return JSON.parse(localStorage.getItem(KEY) ?? 'null') } catch { return null }
}
export const saveSession = (s: Session | null) => {
  try { if (s) localStorage.setItem(KEY, JSON.stringify(s)); else localStorage.removeItem(KEY) } catch { /* modo privado */ }
}

let onUnauthorized: () => void = () => {}
export const setUnauthorizedHandler = (fn: () => void) => { onUnauthorized = fn }

async function call<T>(path: string, init: RequestInit = {}, auth = true): Promise<T> {
  const token = loadSession()?.token
  let res: Response
  try {
    res = await fetch(`${BASE}/api${path}`, {
      ...init,
      headers: { 'Content-Type': 'application/json', ...(auth && token ? { Authorization: `Bearer ${token}` } : {}) },
    })
  } catch {
    throw new ApiError('Não foi possível conectar ao servidor.', 0)
  }
  if (res.status === 401 && auth) onUnauthorized()
  if (!res.ok) {
    let body: { message?: string; fields?: Record<string, string> } = {}
    try { body = await res.json() } catch { /* vazio */ }
    throw new ApiError(body.message ?? (res.status === 401 ? 'E-mail ou senha incorretos.' : 'Algo deu errado.'), res.status, body.fields)
  }
  return res.status === 204 ? (undefined as T) : res.json()
}
const json = (method: string, body: unknown): RequestInit => ({ method, body: JSON.stringify(body) })

export const owner = {
  login: (email: string, password: string) => call<Session>('/auth/login', json('POST', { email, password }), false),
  register: (b: { businessName: string; slug: string; email: string; password: string }) =>
    call<Session>('/auth/register', json('POST', b), false),

  services: () => call<Service[]>('/services'),
  createService: (b: { name: string; durationMin: number; priceCents: number }) => call<Service>('/services', json('POST', b)),
  updateService: (id: number, b: { name: string; durationMin: number; priceCents: number; active?: boolean }) =>
    call<Service>(`/services/${id}`, json('PUT', b)),

  professionals: () => call<Professional[]>('/professionals'),
  createProfessional: (b: { name: string; serviceIds: number[] }) => call<Professional>('/professionals', json('POST', b)),
  updateProfessional: (id: number, b: { name: string; serviceIds: number[]; active?: boolean }) =>
    call<Professional>(`/professionals/${id}`, json('PUT', b)),
  saveHours: (id: number, hours: WorkingHours[]) => call<WorkingHours[]>(`/professionals/${id}/working-hours`, json('PUT', hours)),

  appointments: (date: string, professionalId?: number) =>
    call<Appointment[]>(`/appointments?date=${date}${professionalId ? `&professionalId=${professionalId}` : ''}`),
  cancelAppointment: (id: number) => call<Appointment>(`/appointments/${id}/cancel`, { method: 'POST' }),
}
