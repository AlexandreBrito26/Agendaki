import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, type Appointment, type Catalog } from '../api'
import DateTimePicker from '../components/DateTimePicker'
import { formatLongDate } from '../lib/format'

export default function ManagePage() {
  const { slug = '', token = '' } = useParams()
  const [appt, setAppt] = useState<Appointment | null>(null)
  const [catalog, setCatalog] = useState<Catalog | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [mode, setMode] = useState<'view' | 'reschedule' | 'confirm-cancel'>('view')
  const [date, setDate] = useState<string | null>(null)
  const [time, setTime] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  useEffect(() => {
    Promise.all([api.get(token), api.catalog(slug)])
      .then(([a, c]) => { setAppt(a); setCatalog(c) })
      .catch(e => setError(e.message))
  }, [token, slug])

  const run = useCallback(async (fn: () => Promise<Appointment>, okMsg: string) => {
    setBusy(true); setActionError(null)
    try {
      setAppt(await fn()); setMode('view'); setNotice(okMsg)
    } catch (e) {
      setActionError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }, [])

  if (error) {
    return (
      <main className="shell shell--narrow">
        <h1 className="brand">Agendaki</h1>
        <div className="card stack"><h2>Link inválido</h2><p className="muted">{error}</p><Link className="btn" to={`/${slug}`}>Fazer novo agendamento</Link></div>
      </main>
    )
  }
  if (!appt || !catalog) return <main className="shell"><p className="muted center">Carregando…</p></main>

  const cancelled = appt.status === 'CANCELLED'

  return (
    <main className="shell shell--narrow">
      <header className="hero">
        <span className="brand">Agendaki</span>
        <h1>{catalog.businessName}</h1>
      </header>

      <section className="card stack">
        <div className="row">
          <h2>Seu agendamento</h2>
          <span className={`badge ${cancelled ? 'badge--off' : 'badge--ok'}`}>{cancelled ? 'Cancelado' : 'Confirmado'}</span>
        </div>
        {notice && <p className="alert">{notice}</p>}
        <dl className="summary">
          <div><dt>Serviço</dt><dd>{appt.serviceName}</dd></div>
          <div><dt>Profissional</dt><dd>{appt.professionalName}</dd></div>
          <div><dt>Quando</dt><dd className="cap">{formatLongDate(appt.date)}, {appt.startTime} – {appt.endTime}</dd></div>
          <div><dt>Em nome de</dt><dd>{appt.customerName}</dd></div>
        </dl>
        {actionError && <p className="alert alert--error">{actionError}</p>}

        {!cancelled && mode === 'view' && (
          <div className="actions">
            <button className="btn btn--primary" onClick={() => { setMode('reschedule'); setNotice(null) }}>Remarcar</button>
            <button className="btn btn--danger" onClick={() => { setMode('confirm-cancel'); setNotice(null) }}>Cancelar</button>
          </div>
        )}

        {mode === 'confirm-cancel' && (
          <div className="alert alert--error stack">
            <strong>Cancelar este agendamento?</strong>
            <div className="actions">
              <button className="btn btn--danger" disabled={busy} onClick={() => run(() => api.cancel(token), 'Agendamento cancelado.')}>
                {busy ? 'Cancelando…' : 'Sim, cancelar'}
              </button>
              <button className="btn" onClick={() => setMode('view')}>Manter</button>
            </div>
          </div>
        )}

        {mode === 'reschedule' && (
          <div className="stack">
            <h3>Escolha o novo horário</h3>
            <DateTimePicker
              slug={slug}
              timezone={catalog.timezone}
              serviceId={appt.serviceId}
              professionalId={appt.professionalId}
              date={date}
              time={time}
              onChange={(d, t) => { setDate(d); setTime(t) }}
            />
            <div className="actions">
              <button
                className="btn btn--primary"
                disabled={!date || !time || busy}
                onClick={() => run(() => api.reschedule(token, date!, time!), 'Agendamento remarcado!')}
              >
                {busy ? 'Remarcando…' : 'Confirmar novo horário'}
              </button>
              <button className="btn" onClick={() => setMode('view')}>Voltar</button>
            </div>
          </div>
        )}

        {cancelled && <Link className="btn btn--primary" to={`/${slug}`}>Agendar novamente</Link>}
      </section>
    </main>
  )
}
