export const formatPrice = (cents: number) =>
  (cents / 100).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })

export const formatDuration = (min: number) => {
  const h = Math.floor(min / 60)
  const m = min % 60
  if (!h) return `${m} min`
  return m ? `${h} h ${m} min` : `${h} h`
}

/** Hoje (yyyy-mm-dd) no fuso do negócio. */
export const todayIn = (timezone: string) => {
  try {
    return new Intl.DateTimeFormat('en-CA', { timeZone: timezone }).format(new Date())
  } catch {
    return new Intl.DateTimeFormat('en-CA').format(new Date())
  }
}

/** Soma dias a um yyyy-mm-dd sem passar por fuso (usa UTC como calendário puro). */
export const addDays = (iso: string, days: number) => {
  const [y, m, d] = iso.split('-').map(Number)
  const dt = new Date(Date.UTC(y, m - 1, d + days))
  return dt.toISOString().slice(0, 10)
}

const asUtc = (iso: string) => {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(Date.UTC(y, m - 1, d))
}

export const weekdayShort = (iso: string) =>
  asUtc(iso).toLocaleDateString('pt-BR', { weekday: 'short', timeZone: 'UTC' }).replace('.', '')

export const dayNumber = (iso: string) => Number(iso.slice(8, 10))

export const monthShort = (iso: string) =>
  asUtc(iso).toLocaleDateString('pt-BR', { month: 'short', timeZone: 'UTC' }).replace('.', '')

export const formatLongDate = (iso: string) =>
  asUtc(iso).toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long', timeZone: 'UTC' })

/** Máscara simples de telefone brasileiro: (11) 91234-5678. */
export const maskPhone = (value: string) => {
  const d = value.replace(/\D/g, '').slice(0, 11)
  if (d.length <= 2) return d
  if (d.length <= 6) return `(${d.slice(0, 2)}) ${d.slice(2)}`
  if (d.length <= 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`
  return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`
}

/** Link do WhatsApp (wa.me) com mensagem pronta. Assume DDI 55 quando o número não o traz. */
export const whatsappUrl = (phone: string, message: string) => {
  let digits = phone.replace(/\D/g, '')
  if (!(digits.startsWith('55') && digits.length >= 12)) digits = `55${digits}`
  return `https://wa.me/${digits}?text=${encodeURIComponent(message)}`
}

/** Mensagem de lembrete enviada pelo dono ao cliente. */
export const reminderMessage = (a: {
  customerName: string; serviceName: string; professionalName: string; date: string; startTime: string
}, businessName: string) =>
  `Olá, ${a.customerName.trim().split(/\s+/)[0]}! Passando para lembrar do seu horário na ${businessName}: ` +
  `${a.serviceName} com ${a.professionalName}, ${formatLongDate(a.date)} às ${a.startTime}. ` +
  `Posso confirmar sua presença?`
