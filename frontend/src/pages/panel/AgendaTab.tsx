import { useEffect, useState } from 'react'
import type { Appointment } from '../../api'
import { useAuth } from '../../authContext'
import { addDays, formatLongDate, reminderMessage, whatsappUrl } from '../../lib/format'
import { owner, type Professional } from '../../ownerApi'

const localToday = () => new Intl.DateTimeFormat('en-CA').format(new Date())

export default function AgendaTab() {
  const { session } = useAuth()
  const [date, setDate] = useState(localToday)
  const [profId, setProfId] = useState<number | undefined>()
  const [pros, setPros] = useState<Professional[]>([])
  const [result, setResult] = useState<{ key: string; list: Appointment[] } | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [confirming, setConfirming] = useState<number | null>(null)
  const [reloads, setReloads] = useState(0)

  useEffect(() => { owner.professionals().then(setPros).catch(() => {}) }, [])

  // O resultado carrega a chave (dia + profissional) a que pertence: se a chave mudou, ainda estamos carregando.
  const key = `${date}|${profId ?? ''}`
  useEffect(() => {
    let ignore = false
    owner.appointments(date, profId)
      .then(l => { if (!ignore) { setResult({ key, list: l }); setError(null) } })
      .catch(e => { if (!ignore) setError(e.message) })
    return () => { ignore = true }
  }, [date, profId, key, reloads])
  const list = result?.key === key ? result.list : null

  const cancel = async (id: number) => {
    try { await owner.cancelAppointment(id); setConfirming(null); setReloads(n => n + 1) } catch (e) { setError((e as Error).message) }
  }
  const active = list?.filter(a => a.status === 'CONFIRMED') ?? []

  return (
    <section className="stack">
      <div className="toolbar">
        <div className="datenav">
          <button className="btn btn--small" onClick={() => setDate(addDays(date, -1))} aria-label="Dia anterior">‹</button>
          <input type="date" value={date} onChange={e => e.target.value && setDate(e.target.value)} aria-label="Data" />
          <button className="btn btn--small" onClick={() => setDate(addDays(date, 1))} aria-label="Próximo dia">›</button>
          <button className="btn btn--small" onClick={() => setDate(localToday())}>Hoje</button>
        </div>
        <select value={profId ?? ''} onChange={e => setProfId(e.target.value ? Number(e.target.value) : undefined)} aria-label="Profissional">
          <option value="">Todos os profissionais</option>
          {pros.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
        </select>
      </div>
      <h2 className="cap">{formatLongDate(date)} <span className="muted">· {active.length} agendamento{active.length === 1 ? '' : 's'}</span></h2>
      {error && <p className="alert alert--error">{error}</p>}
      {list === null && !error && <p className="muted">Carregando…</p>}
      {list?.length === 0 && <div className="card empty">Nenhum agendamento neste dia.</div>}
      <ul className="agenda">
        {list?.map(a => (
          <li key={a.id} className={`card appt${a.status === 'CANCELLED' ? ' appt--off' : ''}`}>
            <div className="appt__time">{a.startTime}<small>{a.endTime}</small></div>
            <div className="appt__main">
              <strong>{a.customerName}</strong>
              <span>{a.serviceName} · {a.professionalName}</span>
              <a className="muted" href={whatsappUrl(a.customerPhone, '')} target="_blank" rel="noreferrer">{a.customerPhone}</a>
            </div>
            {a.status === 'CANCELLED' ? <span className="badge badge--off">Cancelado</span>
              : confirming === a.id ? (
                <div className="actions">
                  <button className="btn btn--small btn--danger" onClick={() => cancel(a.id)}>Confirmar</button>
                  <button className="btn btn--small" onClick={() => setConfirming(null)}>Não</button>
                </div>
              ) : (
                <div className="actions">
                  <a className="btn btn--small" target="_blank" rel="noreferrer"
                    href={whatsappUrl(a.customerPhone, reminderMessage(a, session?.businessName ?? ''))}>Lembrar no WhatsApp</a>
                  <button className="btn btn--small" onClick={() => setConfirming(a.id)}>Cancelar</button>
                </div>
              )}
          </li>
        ))}
      </ul>
    </section>
  )
}
