/** Links para o cliente guardar o agendamento no calendário dele. */
interface Evt { serviceName: string; professionalName: string; startsAt?: string; endsAt?: string }

const stamp = (iso: string) => new Date(iso).toISOString().replace(/[-:]|\.\d{3}/g, '') // 20261026T130000Z

export const canAddToCalendar = (a: Evt) => Boolean(a.startsAt && a.endsAt)

export const googleCalendarUrl = (a: Evt, businessName: string) =>
  'https://calendar.google.com/calendar/render?' + new URLSearchParams({
    action: 'TEMPLATE',
    text: `${a.serviceName} · ${businessName}`,
    dates: `${stamp(a.startsAt!)}/${stamp(a.endsAt!)}`,
    details: `Com ${a.professionalName}. Agendado pelo Agendaki.`,
  }).toString()

/** Conteúdo de um arquivo .ics (abre em qualquer app de calendário). */
export const icsContent = (a: Evt, businessName: string) => [
  'BEGIN:VCALENDAR', 'VERSION:2.0', 'PRODID:-//Agendaki//PT-BR', 'BEGIN:VEVENT',
  `UID:${stamp(a.startsAt!)}-${Math.random().toString(36).slice(2)}@agendaki`,
  `DTSTAMP:${stamp(new Date().toISOString())}`,
  `DTSTART:${stamp(a.startsAt!)}`, `DTEND:${stamp(a.endsAt!)}`,
  `SUMMARY:${a.serviceName} · ${businessName}`,
  `DESCRIPTION:Com ${a.professionalName}`,
  'END:VEVENT', 'END:VCALENDAR',
].join('\r\n')

export function downloadIcs(a: Evt, businessName: string) {
  const url = URL.createObjectURL(new Blob([icsContent(a, businessName)], { type: 'text/calendar' }))
  const link = Object.assign(document.createElement('a'), { href: url, download: 'agendamento.ics' })
  document.body.appendChild(link); link.click(); link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
